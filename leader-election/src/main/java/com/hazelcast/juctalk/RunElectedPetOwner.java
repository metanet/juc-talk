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
import static com.hazelcast.juctalk.PrimitiveNames.PHOTO_REF_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.NOTIFIER_LATCH_NAME;
import static com.hazelcast.juctalk.RunPetOwner.getPet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * This class starts a pet owner. You can start multiple pet owners to achieve
 * fault tolerance. Once a pet owner is started, it first attempts to acquire
 * a {@link FencedLock} denoted via {@link RunElectedPetOwner#LOCK_NAME}.
 * When multiple pet owners are started, the pet owner which acquired
 * the lock becomes the leader. All other pet owners wait on the
 * {@link FencedLock#lock()} call until the lock-acquired pet owner releases
 * the lock or fails.
 * <p>
 * An elected pet owner periodically creates a new {@link Photo} and publishes
 * it through a linearizable {@link IAtomicReference} instance. It also counts
 * down the {@link PrimitiveNames#NOTIFIER_LATCH_NAME} latch to notify
 * the parties that are reading published {@link Photo} objects.
 */
public class RunElectedPetOwner {

    public static final String LOCK_NAME = "lock";

    public static void main(String[] args) {
        String pet = getPet(args);

        ClientConfig config = new YamlClientConfigBuilder().build();
        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        ILogger logger = client.getLoggingService().getLogger(RunElectedPetOwner.class);
        String address = client.getLocalEndpoint().getSocketAddress().toString();
        CPSubsystem cpSubsystem = client.getCPSubsystem();

        FencedLock lock = cpSubsystem.getLock(LOCK_NAME);
        IAtomicReference<Photo> photoRef = cpSubsystem.getAtomicReference(PHOTO_REF_NAME);
        ICountDownLatch notifier = cpSubsystem.getCountDownLatch(NOTIFIER_LATCH_NAME);

        try {
            logger.info("PetOwner<" + address + "> is attempting to acquire the lock!");
            lock.lock();
            logger.info("PetOwner<" + address + "> acquired the lock and became the leader!");

            notifier.trySetCount(1);
            Random random = new Random();

            while (true) {
                Photo currentPhoto = photoRef.get();
                int nextVersion = currentPhoto != null ? currentPhoto.getId() + 1 : 1;
                int petIndex = 1 + random.nextInt(15);
                Photo newPhoto = new Photo(nextVersion, pet + petIndex + ".png");
                photoRef.compareAndSet(currentPhoto, newPhoto);

                logger.info("PetOwner<" + address + "> published " + newPhoto);

                notifier.countDown();
                notifier.trySetCount(1);

                sleepUninterruptibly(2000 + random.nextInt(1000), MILLISECONDS);
            }
        } finally {
            lock.unlock();
            client.shutdown();
        }
    }

}
