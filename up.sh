#!/usr/bin/env bash

dir="pet-owner"

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer &)
(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunServer &)
