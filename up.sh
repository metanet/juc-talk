#!/usr/bin/env bash

dir="pet-owner"

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
