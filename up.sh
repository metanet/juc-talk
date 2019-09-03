#!/usr/bin/env bash

touch log.txt

dir="pet-owner"

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer > /dev/null &)

tail -n 200 -f log.txt
