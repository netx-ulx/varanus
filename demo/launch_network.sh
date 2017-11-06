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
if ! [ -d "$LOCAL_DIR/../collector/target" -a -d "$LOCAL_DIR/../sdncontroller/target" ]; then
    abort "Must build collector and sdncontroller applications first"
fi

MININET_RUN="$LOCAL_DIR/../network-manager/run_mininet.py"
msg "Launching network manager..."
"$MININET_RUN" --arp --config demo --autocfg

