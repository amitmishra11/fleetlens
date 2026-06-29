package com.fleetlens.embeddedkafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Runs a real (in-process) single-broker Kafka cluster so the rest of FleetLens can
 * talk to an ordinary Kafka bootstrap address without Docker, Zookeeper, or any native
 * Kafka install. The KRaft test broker picks its own free port rather than honouring a
 * fixed one, so the actual bootstrap address is written to a small file that other
 * processes (demo-service, api-gateway) read at startup instead of hardcoding a port.
 */
public final class EmbeddedKafkaMain {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafkaMain.class);
    private static final String DEFAULT_BOOTSTRAP_FILE = ".fleetlens-run/kafka-bootstrap.txt";

    public static void main(String[] args) throws InterruptedException, IOException {
        Path bootstrapFile = Path.of(args.length > 0 ? args[0] : DEFAULT_BOOTSTRAP_FILE);
        Files.createDirectories(bootstrapFile.toAbsolutePath().getParent());

        EmbeddedKafkaBroker broker = new EmbeddedKafkaKraftBroker(1, 1, "order-events");

        log.info("Starting embedded Kafka broker...");
        broker.afterPropertiesSet();
        String bootstrapServers = broker.getBrokersAsString();
        log.info("Embedded Kafka broker is up. Bootstrap servers: {}", bootstrapServers);

        Files.writeString(bootstrapFile, bootstrapServers);
        log.info("Wrote bootstrap address to {}", bootstrapFile.toAbsolutePath());

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down embedded Kafka broker...");
            broker.destroy();
            try {
                Files.deleteIfExists(bootstrapFile);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
            shutdownLatch.countDown();
        }));

        shutdownLatch.await();
    }

    private EmbeddedKafkaMain() {
    }
}
