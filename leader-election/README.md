# Leader Election Use-Case

This use-case extends the Pet-Photo use-case. In this one, we make
pet owners fault tolerant by running multiple pet owner processes and electing
a leader among them by using a `FencedLock`.

In this use-case, we run the CP Subsystem with 3 CP members. While in the
project root directory, hit `./up.sh` to start 3 Hazelcast members. They will
run in the background and initialize the CP Subsystem. You can `tail` on 
`log.txt` to track the progress. After the Hazelcast servers are started, hit 
`pet-web-app/run.sh &` to start the pet service. In this use case, we
configure CP session time-to-live duration to 15 seconds. This is not a good 
configuration for production workloads and we use it here only for
demonstration purposes.

We will make 3 runs of this use-case.

- In the first run, we run pet owners by hitting 
`leader-election/runElectedPetOwner.sh cat|dog`. Run this command in the
project root directory once with `cat` argument and once with `dog` argument.
Then, you will have a cat owner and a dog owner, and see that one of them will
acquire the FencedLock lock and start posting new pet photos. You can observe 
in `http://localhost:8080/index.html` that only a single pet owner is posting
photos. You should be seeing either only cat photos or dog photos. Then, hit
`CTRL + C` to stop the process of the leader pet owner. The lock ownership of
the stopped pet owner will be cancelled after approximately 15 seconds and 
the other pet owner node will became the new leader. If you were observing cat
photos in `http://localhost:8080/index.html` before stopping the leader pet 
owner, you will start to see dog photos, and vice versa.   
  
- In the second run, we perform the same initial steps and run 2 pet owners.
Then, instead of stopping the leader pet owner, we pause its process by hitting 
`CTRL + Z` and wait until its lock ownership is eventually cancelled and
the lock is assigned to the other pet owner. Then, resume the paused process
by hitting `fg`. Now, we have 2 pet owner processes both thinking they hold
the lock and posting pet photos. You can observe in 
`http://localhost:8080/index.html` that the photos are alternating between cats
and dogs. 

- In the third run, we solve this problem by using the fencing tokens returned
by FencedLock. This time, we hit `./runFencingPetOwner.sh` to start our pet
owners and repeat the same "pausing" scenario. In this implementation, pet
owners put their fencing tokens to the posted photos. Moreover, they publish
new photos by using the `IAtomicReference.compareAndSet()` method. When a `CAS`
operation fails, the pet owner checks if the current photo present in the
`IAtomicReference` instance contains a fencing token larger than its own. If it
is the case, the pet owner notices that it has lost ownership of the lock and
is no longer the leader. Hence, it terminates itself. It means that you will
not observe pet photos alternating between cats and dogs in 
`http://localhost:8080/index.html`.
