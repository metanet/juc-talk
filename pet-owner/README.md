# Pet-Photo Management Use-Case

This use-case demonstrates how we can manage configuration information
in our applications by using the concurrency primitives of the CP Subsystem.
We do it by running a scenario where there is a pet owner publishing pet photos
and a web application displaying those pictures.

In this use-case, we run the CP Subsystem with 3 CP members. While in the
project root directory, hit `./up.sh` to start 3 Hazelcast members. 
They will run in the background and initialize the CP Subsystem. You can `tail`
on `log.txt` to track the progress.

In this application, a pet owner can publish cat photos or dog photos. The pet
owner publishes a new photo by using an `IAtomicReference` instance. Then, it
counts-down a `ICountDownLatch` instance to notify pet services. Dually, a pet
service awaits on the `ICountDownLatch` to get notified immediately when a new
pet photo is published by the pet owner. 

You can start a pet owner via hitting `pet-owner/run.sh cat` or 
`pet-owner/run.sh dog`, and the pet service via hitting `pet-web-app/run.sh`
from the project root directory. Once the pet service web application is up and
running, just hit `http://localhost:8080/index.html` in your browser. You will
see that new pet photos are being published every 2-3 seconds. 

Once you are done you can hit `./down.sh` to terminate all Hazelcast processes.

 
