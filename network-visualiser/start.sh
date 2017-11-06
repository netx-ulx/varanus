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
else
    msg "Note: using default tomcat home directory '$DEFAULT_TOMCAT_HOME'"
    TOMCAT_HOME="$DEFAULT_TOMCAT_HOME"
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

if "$TOMCAT_STARTUP"; then
    tail -f "$CATALINA_BASE/logs/catalina.out"
else
    abort "Error while starting up tomcat server"
fi

