package com.fleetlens.common.registry;

import java.util.List;

/**
 * A monitored service target. jmxHost/jmxPort are optional - a service with
 * neither set simply won't be heap-profiled, but config auditing and Kafka
 * lag tracking (if kafkaConsumerGroups is non-empty) still work.
 */
public record ServiceDefinition(
        String id,
        String baseUrl,
        String env,
        String jmxHost,
        Integer jmxPort,
        List<String> kafkaConsumerGroups,
        boolean managed) {

    public ServiceDefinition {
        kafkaConsumerGroups = kafkaConsumerGroups == null ? List.of() : List.copyOf(kafkaConsumerGroups);
        env = env == null || env.isBlank() ? "dev" : env;
    }
}
