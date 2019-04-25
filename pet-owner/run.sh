#!/usr/bin/env bash

if [ $# != 1 ]; then
	echo "how to use: ./run.sh cat|dog"
	exit 1
fi

dir=`dirname "$0"`

(cd $dir && mvn exec:java -Dexec.mainClass=com.hazelcast.juctalk.RunPetOwner -Dexec.args=$1)
