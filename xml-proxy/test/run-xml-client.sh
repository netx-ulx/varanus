#!/usr/bin/env bash

BASE_DIR="$(readlink -f "$(dirname $0)")"
TARGET_DIR="${BASE_DIR}/../target"

XML_PROXY_JAR="${TARGET_DIR}/varanus-xmlproxy.jar"
XML_CLIENT_CLASS="net.varanus.xmlproxy.test.XMLSimpleClient"

java -cp "$XML_PROXY_JAR" "$XML_CLIENT_CLASS"

