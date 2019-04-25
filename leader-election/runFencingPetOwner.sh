#!/usr/bin/env bash

if [ $# != 1 ]; then
	echo "how to use: ./runFencingPetOwner.sh cat|dog"
	exit 1
fi

dir=`dirname "$0"`

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunFencingPetOwner -Dexec.args=$1)
