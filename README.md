The CP Subsystem module of Hazelcast IMDG 3.12 offers linearizable and
distributed implementations of the Java concurrency primitives; `Lock`,
`Semaphore`,`CountDownLatch`, `AtomicReference`, and `AtomicLong`. This repo
contains a couple of code samples to demonstrate how to use them for
distributed coordination use cases. 

We use a simple deployment model in the use-cases. We deploy a cluster of 3
Hazelcast servers with the CP Subsystem enabled. The cluster works like 
a central distributed coordination service. Then, we connect to the Hazelcast
cluster via multiple Hazelcast clients.  

![](diagram.png?raw=true)

Each use-case is implemented as a Maven submodule, except the `server` 
submodule, which contains common utilities for the use-cases. We also provide
a set of scripts to start and terminate Hazelcast servers and clients. Last,
each use-case creates multiple Hazelcast server and client instances, but all
of their logs are printed into a single `log.txt` file in the project root
directory to help you track the progress easily.  

Make sure to hit `mvn clean install` in the project root directory before
running the code samples. 
