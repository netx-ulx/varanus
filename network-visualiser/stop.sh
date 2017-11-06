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

DEFAULT_TOMCAT_HOME="/opt/tomcat"
if [ -n "$CATALINA_HOME" ]; then
    TOMCAT_HOME="$CATALINA_HOME"
else
    msg "Note: using default tomcat home directory '$DEFAULT_TOMCAT_HOME'"
    TOMCAT_HOME="$DEFAULT_TOMCAT_HOME"
fi

TOMCAT_SHUTDOWN="$TOMCAT_HOME/bin/shutdown.sh"

if [ ! -f "$TOMCAT_SHUTDOWN" ]; then
    abort "Could not find tomcat shutdown script '$TOMCAT_SHUTDOWN'"
fi

if "$TOMCAT_SHUTDOWN"; then
    true # do nothing
else
    abort "Error while shutting down tomcat server"
fi

