package com.fleetlens.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order events with an artificial per-message delay. Since the delay
 * exceeds the producer's publish interval by default, consumer lag grows steadily -
 * giving the Memory Profiler's heap/lag correlation panel real movement to show
 * without any extra setup.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final DemoProperties properties;

    public OrderEventConsumer(DemoProperties properties) {
        this.properties = properties;
    }

    @KafkaListener(topics = "${demo.topic:order-events}", groupId = "order-service-group")
    public void onMessage(String payload) {
        try {
            Thread.sleep(properties.getConsumerDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("Processed order event: {}", payload);
    }
}
