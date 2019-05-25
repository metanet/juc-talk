#!/usr/bin/env bash

dir=`dirname "$0"`

(cd ${dir} && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.petapp.PetApplication > /dev/null &)
