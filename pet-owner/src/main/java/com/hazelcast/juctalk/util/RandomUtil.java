package com.hazelcast.juctalk.util;

import java.util.Random;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class RandomUtil {

    private static final Random RANDOM = new Random();

    private RandomUtil() {
    }

    public static int randomInt(int n) {
        return RANDOM.nextInt(n);
    }

    public static void randomSleep() {
        sleepUninterruptibly(2000 + randomInt(1000), MILLISECONDS);
    }

}
