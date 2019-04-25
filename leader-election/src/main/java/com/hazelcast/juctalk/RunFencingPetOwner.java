package com.hazelcast.juctalk;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.logging.ILogger;

import java.util.Random;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.hazelcast.juctalk.RunElectedPetOwner.LOCK_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.PHOTO_REF_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.NOTIFIER_LATCH_NAME;
import static com.hazelcast.juctalk.RunPetOwner.getPet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * This class starts a pet owner. You can start multiple pet owners to achieve
 * fault tolerance. Once a Master node started, it first attempts to acquire
 * a {@link FencedLock} denoted via {@link RunElectedPetOwner#LOCK_NAME}.
 * When multiple pet owners are started, the Master node which acquired
 * the lock becomes the leader. All other pet owners wait on the
 * {@link FencedLock#lockAndGetFence()} ()} call until the lock-acquired pet
 * owner releases the lock or fails.
 * <p>
 * An elected pet owner periodically creates a new {@link Photo} and publishes
 * it through a linearizable {@link IAtomicReference} instance. It also counts
 * down the {@link PrimitiveNames#NOTIFIER_LATCH_NAME} latch to notify
 * the parties that are reading published {@link Photo} objects. Last,
 * the leader-elected pet owner also puts its fencing token into the published
 * {@link Photo} objects. Fencing tokens are used for fencing-off stale leaders
 * when lock ownership of a previously elected pet owner is prematurely
 * cancelled but that node is still alive. A pet owner backs-off when it
 * detects that the current {@link Photo} object has a fencing token that is
 * larger than its own token.
 */
public class RunFencingPetOwner {

    public static void main(String[] args) {
        String pet = getPet(args);

        ClientConfig config = new YamlClientConfigBuilder().build();
        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        ILogger logger = client.getLoggingService().getLogger(RunFencingPetOwner.class);
        String address = client.getLocalEndpoint().getSocketAddress().toString();
        CPSubsystem cpSubsystem = client.getCPSubsystem();

        FencedLock lock = cpSubsystem.getLock(LOCK_NAME);
        IAtomicReference<Photo> photoRef = cpSubsystem.getAtomicReference(PHOTO_REF_NAME);
        ICountDownLatch notifier = cpSubsystem.getCountDownLatch(NOTIFIER_LATCH_NAME);

        try {
            logger.info("PetOwner<" + address + "> is attempting to acquire the lock!");
            long fence = lock.lockAndGetFence();
            logger.info("PetOwner<" + address + "> acquired the lock with fence: " + fence
                    + " and became the leader!");

            notifier.trySetCount(1);
            Random random = new Random();

            while (true) {
                Photo currentPhoto = photoRef.get();

                int nextVersion = 0;
                if (currentPhoto != null) {
                    if (currentPhoto.getFence() > fence) {
                        logger.severe("PetOwner<" + address + "> lost ownership of the lock! Current: "
                                + currentPhoto + ", my fencing token: " + fence);
                        client.getLifecycleService().terminate();
                        return;
                    }

                    nextVersion = currentPhoto.getId() + 1;
                }

                int petIndex = 1 + random.nextInt(15);
                String fileName = pet + petIndex + ".png";
                Photo newPhoto = new Photo(fence, nextVersion, fileName);

                if (photoRef.compareAndSet(currentPhoto, newPhoto)) {
                    logger.info("PetOwner<" + address + "> published " + newPhoto);

                    notifier.countDown();
                    notifier.trySetCount(1);
                }

                sleepUninterruptibly(2000 + random.nextInt(1000), MILLISECONDS);
            }
        } finally {
            lock.unlock();
            client.shutdown();
        }
    }

}
