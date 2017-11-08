#!/usr/bin/env bash

# Set paths
BASE_DIR="$(readlink -f "$(dirname $0)")"
TARGET_DIR="${BASE_DIR}/target"
LIB_DIR="${BASE_DIR}/lib"
CONFIG_DIR="${BASE_DIR}/config"

COLL_JAR="${TARGET_DIR}/varanus-collector.jar"
JPCAP_LIB="${LIB_DIR}/jnetpcap-1.4.r1425"
LOGBACK_FILE="${CONFIG_DIR}/collector-logback.xml"
PROPS_FILE="${CONFIG_DIR}/"


# Set JVM options
JVM_OPTS=""
JVM_OPTS="${JVM_OPTS} -server -d64"
#JVM_OPTS="${JVM_OPTS} -Xmx2g -Xms2g -Xmn800m"
JVM_OPTS="${JVM_OPTS} -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods"
JVM_OPTS="${JVM_OPTS} -XX:MaxInlineSize=8192 -XX:FreqInlineSize=8192"
JVM_OPTS="${JVM_OPTS} -XX:CompileThreshold=1500"


# Set JnetPcap options
JVM_OPTS="${JVM_OPTS} -Djava.library.path=${JPCAP_LIB}"
if ! [ -n "${LD_LIBRARY_PATH}" ]; then
    LD_LIBRARY_PATH="/usr/lib/x86_64-linux-gnu"
fi
if ! [ -n "${JAVA_HOME}" ]; then
    JAVA_HOME="$(dirname "$(dirname "$(dirname "$(readlink -v -e "$(which java)")")")")"
fi

java ${JVM_OPTS} -Dlogback.configurationFile=${LOGBACK_FILE}\
                 -jar "${COLL_JAR}"\
                 -cf "${PROPS_FILE}"\
                 "$@"

