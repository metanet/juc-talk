package com.hazelcast.juctalk.petapp;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.juctalk.Photo;
import com.hazelcast.logging.ILogger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.hazelcast.juctalk.Photo.PHOTO_COMPARATOR;
import static com.hazelcast.juctalk.PrimitiveNames.NOTIFIER_LATCH_NAME;
import static com.hazelcast.juctalk.PrimitiveNames.PHOTO_REF_NAME;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.StreamUtils.copyToByteArray;

@Service
public class PetService {

    private final HazelcastInstance client;
    private volatile Photo currentPhoto;

    public PetService(HazelcastInstance client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        Thread thread = new Thread(this::fetchNewPetPhoto);
        thread.start();
    }

    private void fetchNewPetPhoto() {
        ILogger logger = client.getLoggingService().getLogger(PetService.class);
        CPSubsystem cpSubsystem = client.getCPSubsystem();

        IAtomicReference<Photo> photoRef = cpSubsystem.getAtomicReference(PHOTO_REF_NAME);
        ICountDownLatch notifier = cpSubsystem.getCountDownLatch(NOTIFIER_LATCH_NAME);

        notifier.trySetCount(1);

        while (true) {
            try {
                if (!notifier.await(5, SECONDS)) {
                    logger.fine("no notification on the latch...");
                }
            } catch (InterruptedException e) {
                logger.severe("notifier latch await interrupted.");
                return;
            }

            Photo newPhoto = photoRef.get();
            if (PHOTO_COMPARATOR.compare(newPhoto, currentPhoto) > 0) {
                logger.info("fetched " + newPhoto);
                currentPhoto = newPhoto;
            } else {
                sleepUninterruptibly(100, MILLISECONDS);
            }
        }
    }

    public Photo getCurrentPhoto() {
        return currentPhoto;
    }

    public byte[] getPhoto(String name) throws IOException {
        return copyToByteArray(new ClassPathResource("static/" + name).getInputStream());
    }
}
