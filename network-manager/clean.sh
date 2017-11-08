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
if cd "$LOCAL_DIR"; then
    msg "Cleaning built files..."
    find . -name "*.pyc" -exec rm -vf {} \;
else
    abort "Error: could not clean built files"
fi

