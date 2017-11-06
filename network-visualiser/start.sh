#!/usr/bin/env bash

abort() {
    if [ -n "$1" ]; then
        printf "%s\n" "$1" >&2
    fi
    exit 1
}

msg() {
    if [ -n "$1" ]; then
        printf "%s\n" "$1"
    fi
}

JAVA_EXEC="$(command -v java)"
if [ $? -ne 0 ]; then
    abort "Java must be installed to run tomcat server"
fi

DEFAULT_TOMCAT_HOME="/opt/tomcat"
if [ -n "$CATALINA_HOME" ]; then
    TOMCAT_HOME="$CATALINA_HOME"
    if [ -d "$TOMCAT_HOME" ]; then
        msg "Note: using tomcat home directory '$TOMCAT_HOME'"
    else
        abort "Tomcat directory '$TOMCAT_HOME' passed via CATALINA_HOME variable does not exist"
    fi
else
    TOMCAT_HOME="$DEFAULT_TOMCAT_HOME"
    if [ -d "$TOMCAT_HOME" ]; then
        msg "Note: using default tomcat home directory '$TOMCAT_HOME'"
    else
        abort "Default tomcat directory '$TOMCAT_HOME' does not exist. Install tomcat in the default directory or pass a custom directory via CATALINA_HOME variable"
    fi
fi

TOMCAT_STARTUP="$TOMCAT_HOME/bin/startup.sh"
if [ ! -f "$TOMCAT_STARTUP" ]; then
    abort "Could not find tomcat startup script '$TOMCAT_STARTUP'"
fi

if [ -z "$JRE_HOME" -a -z "$JAVA_HOME" ]; then
    export JRE_HOME="$(dirname "$(dirname "$(dirname "$(readlink -v -e "$JAVA_EXEC")")")")"
fi

LOCAL_DIR="$(readlink -f "$(dirname "$0")")"
export SEGRID_HOME="$LOCAL_DIR"
export CATALINA_BASE="$LOCAL_DIR/catalina"

mkdir -p "$CATALINA_BASE/logs"
mkdir -p "$CATALINA_BASE/temp"

if "$TOMCAT_STARTUP"; then
    tail -f "$CATALINA_BASE/logs/catalina.out"
else
    abort "Error while starting up tomcat server"
fi

