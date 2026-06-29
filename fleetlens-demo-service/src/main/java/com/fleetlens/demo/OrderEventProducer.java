package com.fleetlens.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Publishes synthetic order events. Switching demo.schema-variant from v1 to v2
 * (requires a restart) changes the message shape: drops "status" (breaking removal),
 * widens "amount" from a number to a string (breaking type change), and adds
 * "currency" (non-breaking addition) - so the schema drift detector has something
 * concrete to flag on the next sampling pass.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DemoProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, DemoProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${demo.produce-interval-ms:1000}")
    public void produce() {
        String orderId = UUID.randomUUID().toString();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);

        if ("v2".equalsIgnoreCase(properties.getSchemaVariant())) {
            event.put("customerId", "cust-" + ThreadLocalRandom.current().nextInt(1, 50));
            event.put("amount", String.valueOf(ThreadLocalRandom.current().nextDouble(5, 500)));
            event.put("currency", "USD");
            event.put("createdAt", Instant.now().toString());
        } else {
            event.put("customerId", "cust-" + ThreadLocalRandom.current().nextInt(1, 50));
            event.put("amount", ThreadLocalRandom.current().nextDouble(5, 500));
            event.put("status", "CREATED");
            event.put("createdAt", Instant.now().toString());
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(properties.getTopic(), orderId, json);
        } catch (Exception e) {
            log.warn("Failed to publish order event", e);
        }
    }
}
