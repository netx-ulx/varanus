#!/usr/bin/env bash

TG_HOME="$(readlink -f "$(dirname $0)/..")"
TG_CONFIG="${TG_HOME}/config"
TG_PROPS="${TG_CONFIG}/traffic-generator.json"

CTRLR_IP="localhost"
CTRLR_PORT=8080

# to stop traffic (use "all" to stop all traffic)
FLOW_LINK=""

START_TG_CMD="curl -X POST -d @${TG_PROPS} http://${CTRLR_IP}:${CTRLR_PORT}/wm/varanus/trafficgenerator/start"
STOP_TG_CMD="curl -X POST -d \${FLOW_LINK} http://${CTRLR_IP}:${CTRLR_PORT}/wm/varanus/trafficgenerator/stop"

USAGE="$(basename $0) { start | stop { <flowed-link> | all } }"

if [ $# -gt 0 ]; then
    if [ "$1" = "start" ]; then
        echo "Running: ${START_TG_CMD}"
        eval "${START_TG_CMD}"
        exit 0
    elif [ "$1" = "stop" ]; then
        if [ $# -gt 1 ]; then
            FLOW_LINK="$2"
            echo "Running: ${STOP_TG_CMD}"
            eval "${STOP_TG_CMD}"
            exit 0
        fi
    fi
fi

echo "${USAGE}"
exit 1

