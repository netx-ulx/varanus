#!/usr/bin/env bash

BASE_DIR="$(readlink -f "$(dirname $0)")"
TARGET_DIR="${BASE_DIR}/../target"

XML_PROXY_JAR="${TARGET_DIR}/varanus-xmlproxy.jar"
MN_CLIENT_CLASS="net.varanus.xmlproxy.test.MininetSimpleClient"

java -cp "$XML_PROXY_JAR" "$MN_CLIENT_CLASS"

