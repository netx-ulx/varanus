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

LOCAL_DIR="$(readlink -f "$(dirname "$0")")"
if ! [ -d "$LOCAL_DIR/../xml-proxy/target" ]; then
    abort "Must build xml-proxy application first"
fi

DAEMONIZER="$LOCAL_DIR/../utils/bin/daemonizer.sh"
XMLPROXY_RUN="$LOCAL_DIR/../xml-proxy/run-varanus-xml-proxy.sh"
VISUALISER_START="$LOCAL_DIR/../network-visualiser/start.sh"
VISUALISER_STOP="$LOCAL_DIR/../network-visualiser/stop.sh"

msg "Launching network visualiser..."

msg " "
msg "Starting XML-proxy application..."
"$DAEMONIZER" start "$XMLPROXY_RUN"

msg " "
msg "Starting tomcat server and visualiser application..."
"$DAEMONIZER" start "$VISUALISER_START"

msg " "
read -p "Press Enter to stop network visualiser"

"$DAEMONIZER" stop "$VISUALISER_START"
msg " "
msg "Stopping tomcat server..."
"$VISUALISER_STOP"

msg " "
msg "Stopping XML-proxy application..."
"$DAEMONIZER" stop "$XMLPROXY_RUN"

