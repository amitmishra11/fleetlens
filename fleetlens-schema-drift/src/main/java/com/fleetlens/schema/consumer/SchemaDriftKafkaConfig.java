package com.fleetlens.schema.consumer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Kafka admin/consumer beans used purely for topic listing and short-lived sampling. These are
 * intentionally separate from any application-wide Kafka consumer factory so the schema-drift
 * module stays self-contained when embedded in the API gateway.
 */
@Configuration
public class SchemaDriftKafkaConfig {

    @Bean
    @ConditionalOnMissingBean(name = "schemaDriftAdminClient")
    public AdminClient schemaDriftAdminClient(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    @ConditionalOnMissingBean(name = "schemaDriftSamplerConsumerFactory")
    public SamplerConsumerFactory schemaDriftSamplerConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return () -> new KafkaConsumer<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.GROUP_ID_CONFIG, "fleetlens-schema-drift-sampler",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        ));
    }

    /**
     * Each sampling pass needs its own short-lived consumer (fresh group, fresh subscription)
     * rather than one long-lived shared consumer, so this factory creates a new one per call.
     */
    @FunctionalInterface
    public interface SamplerConsumerFactory {
        KafkaConsumer<String, String> create();
    }
}
