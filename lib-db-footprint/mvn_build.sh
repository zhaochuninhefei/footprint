#!/bin/bash

export JAVA_HOME=/usr/java/jdk-11.0.7+10

mvn -version

mvn clean install package -U
