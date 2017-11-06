#!/usr/bin/env bash

# Set paths
BASE_DIR="$(readlink -f "$(dirname $0)")"
TARGET_DIR="${BASE_DIR}/target"
LIB_DIR="${BASE_DIR}/lib"
CONFIG_DIR="${BASE_DIR}/config"

XML_PROXY_JAR="${TARGET_DIR}/varanus-xmlproxy.jar"
LOGBACK_FILE="${CONFIG_DIR}/varanus-xmlproxy-logback.xml"
PROPS_FILE="${CONFIG_DIR}/varanus-xmlproxy.properties"


# Set JVM options
JVM_OPTS=""
JVM_OPTS="${JVM_OPTS} -server -d64"
JVM_OPTS="${JVM_OPTS} -Xmx2g -Xms2g -Xmn800m"
JVM_OPTS="${JVM_OPTS} -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods"
JVM_OPTS="${JVM_OPTS} -XX:MaxInlineSize=8192 -XX:FreqInlineSize=8192"
JVM_OPTS="${JVM_OPTS} -XX:CompileThreshold=1500"


java ${JVM_OPTS} -Dlogback.configurationFile=${LOGBACK_FILE}\
                 -jar "${XML_PROXY_JAR}"\
                 -cf "${PROPS_FILE}"\
                 "$@"

