package com.fleetlens.config.api;

import java.util.List;

public record RegisterServiceRequest(
        String id,
        String baseUrl,
        String env,
        String jmxHost,
        Integer jmxPort,
        List<String> kafkaConsumerGroups) {
}
