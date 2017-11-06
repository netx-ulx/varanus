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

[ $EUID -eq 0 ] || abort "Must run as root"

LOCAL_DIR="$(readlink -f "$(dirname "$0")")"
MININET_RUN="$LOCAL_DIR/../network-manager/run_mininet.py"

msg "Cleaning up network manager..."
"$MININET_RUN" --config demo --cleanlocal

