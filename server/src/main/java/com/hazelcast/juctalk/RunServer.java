package com.hazelcast.juctalk;

import com.hazelcast.config.Config;
import com.hazelcast.config.YamlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.logging.ILogger;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Starts a Hazelcast member and prints a log after the CP Subsystem discovery
 * process is completed.
 */
public class RunServer {

    public static void main(String[] args) throws InterruptedException {
        Config config = new YamlConfigBuilder().build();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        instance.getCPSubsystem()
                .getCPSubsystemManagementService()
                .awaitUntilDiscoveryCompleted(2, MINUTES);

        ILogger logger = instance.getLoggingService().getLogger(RunServer.class);
        logger.info("ready to go!");
    }

}
