package com.hazelcast.juctalk;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.logging.ILogger;

import static com.hazelcast.juctalk.Photo.getRandomPhotoFileName;
import static com.hazelcast.juctalk.PrimitiveNames.NOTIFIER_LATCH_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.PHOTO_REF_NAME;
import static com.hazelcast.juctalk.util.RandomUtil.randomSleep;

/**
 * This class starts a pet owner.
 * <p>
 * A pet owner periodically creates a pet {@link Photo} and publishes it
 * through a linearizable {@link IAtomicReference} instance. It also counts
 * down the {@link PrimitiveNames#NOTIFIER_LATCH_NAME} latch to notify
 * the parties that are reading published {@link Photo} objects.
 */
public class RunPetOwner {

    public static void main(String[] args) {
        String pet = parsePet(args);

        ClientConfig config = new YamlClientConfigBuilder().build();
        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        ILogger logger = client.getLoggingService().getLogger(RunPetOwner.class);
        String address = client.getLocalEndpoint().getSocketAddress().toString();
        CPSubsystem cpSubsystem = client.getCPSubsystem();

        IAtomicReference<Photo> photoRef = cpSubsystem.getAtomicReference(PHOTO_REF_NAME);
        ICountDownLatch notifier = cpSubsystem.getCountDownLatch(NOTIFIER_LATCH_NAME);

        notifier.trySetCount(1);

        while (true) {
            Photo currentPhoto = photoRef.get();
            int nextVersion = currentPhoto != null ? currentPhoto.getId() + 1 : 1;
            Photo newPhoto = new Photo(nextVersion, getRandomPhotoFileName(pet));
            photoRef.set(newPhoto);

            logger.info("PetOwner<" + address + "> published " + newPhoto);

            notifier.countDown();
            notifier.trySetCount(1);

            randomSleep();
        }
    }

    public static String parsePet(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("You must provide a single argument: cat|dog");
        }

        String pet = args[0].trim().toLowerCase();
        if (!(pet.equals("cat") || pet.equals("dog"))) {
            throw new IllegalArgumentException("You must provide a single argument: cat|dog");
        }

        return pet;
    }

}
