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
import static com.hazelcast.juctalk.RunElectedPetOwner.LOCK_NAME;
import static com.hazelcast.juctalk.RunPetOwner.parsePet;
import static com.hazelcast.juctalk.RunPetOwner.toEmoji;
import static com.hazelcast.juctalk.util.RandomUtil.randomSleep;

/**
 * This class starts a pet owner. You can start multiple pet owners to achieve
 * redundancy. Once a Master node started, it first attempts to acquire
 * a {@link FencedLock} denoted via {@link RunElectedPetOwner#LOCK_NAME}.
 * When multiple pet owners are started, the Master node which acquired
 * the lock becomes the leader. All other pet owners wait on the
 * {@link FencedLock#lockAndGetFence()} ()} call until the lock-acquired pet
 * owner releases the lock or fails.
 * <p>
 * An elected pet owner periodically creates a new {@link Photo} and posts it
 * through a linearizable {@link IAtomicReference} instance. It also counts
 * down the {@link PrimitiveNames#NOTIFIER_LATCH_NAME} latch to notify
 * the parties that are reading published {@link Photo} objects. Last,
 * the leader-elected pet owner also puts its fencing token into the
 * {@link Photo} objects. Fencing tokens are used for fencing-off stale leaders
 * when lock ownership of a previously elected pet owner is prematurely
 * cancelled but that node is still alive. A pet owner backs-off when it
 * detects that the current {@link Photo} object has a fencing token that is
 * larger than its own token.
 */
public class RunFencingPetOwner {

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
            long fence = lock.lockAndGetFence();
            logger.info("acquired the lock with fence: " + fence + " and became the leader!");

            notifier.trySetCount(1);

            while (true) {
                Photo currentPhoto = photoRef.get();

                int nextVersion = 0;
                if (currentPhoto != null) {
                    if (currentPhoto.getFence() > fence) {
                        logger.severe("lost the leadership! my token: " + fence + ", current: " + currentPhoto);
                        client.getLifecycleService().terminate();
                        return;
                    }

                    nextVersion = currentPhoto.getId() + 1;
                }

                Photo newPhoto = new Photo(fence, nextVersion, getRandomPhotoFileName(pet));

                if (photoRef.compareAndSet(currentPhoto, newPhoto)) {
                    logger.info("posted new " + newPhoto);

                    notifier.countDown();
                    notifier.trySetCount(1);
                }

                randomSleep();
            }
        } finally {
            lock.unlock();
            client.shutdown();
        }
    }

}
