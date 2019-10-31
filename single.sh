#!/usr/bin/env bash

touch log.txt

dir="pet-owner"

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
