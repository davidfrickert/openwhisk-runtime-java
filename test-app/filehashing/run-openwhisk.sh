#!/bin/bash

export WSK_CONFIG_FILE=/home/rbruno/git/incubator-openwhisk-devtools/docker-compose/.wskprops
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export OPENWHISK_HOME=/home/rbruno/git/openwhisk

wsk action --result  -i invoke FileHashing  --param seed 1234
