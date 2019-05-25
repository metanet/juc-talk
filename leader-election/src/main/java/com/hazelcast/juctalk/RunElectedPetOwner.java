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

import static com.hazelcast.juctalk.Photo.getRandomPhotoFileName;
import static com.hazelcast.juctalk.PrimitiveNames.NOTIFIER_LATCH_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.PHOTO_REF_NAME;
import static com.hazelcast.juctalk.RunPetOwner.parsePet;
import static com.hazelcast.juctalk.RunPetOwner.toEmoji;
import static com.hazelcast.juctalk.util.RandomUtil.randomSleep;

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
        String pet = parsePet(args);

        ClientConfig config = new YamlClientConfigBuilder().build();
        config.setInstanceName(toEmoji(pet));
        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        ILogger logger = client.getLoggingService().getLogger("PetOwner");
        CPSubsystem cpSubsystem = client.getCPSubsystem();

        FencedLock lock = cpSubsystem.getLock(LOCK_NAME);
        IAtomicReference<Photo> photoRef = cpSubsystem.getAtomicReference(PHOTO_REF_NAME);
        ICountDownLatch notifier = cpSubsystem.getCountDownLatch(NOTIFIER_LATCH_NAME);

        try {
            logger.info("attempting to acquire the lock!");
            lock.lock();
            logger.info("acquired the lock and became the leader!");

            notifier.trySetCount(1);

            while (true) {
                Photo currentPhoto = photoRef.get();
                int nextVersion = currentPhoto != null ? currentPhoto.getId() + 1 : 1;
                Photo newPhoto = new Photo(nextVersion, getRandomPhotoFileName(pet));

                // even if we check the lock ownership here and get into the if block,
                // we might fall into a GC pause and lose the lock before posting the new photo
                // if (lock.isLockedByCurrentThread()) {
                    photoRef.compareAndSet(currentPhoto, newPhoto);
                // }

                logger.info("posted new " + newPhoto);

                notifier.countDown();
                notifier.trySetCount(1);

                randomSleep();
            }
        } finally {
            lock.unlock();
            client.shutdown();
        }
    }

}
