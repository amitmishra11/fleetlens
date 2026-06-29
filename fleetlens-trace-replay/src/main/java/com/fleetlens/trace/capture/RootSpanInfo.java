package com.fleetlens.trace.capture;

public record RootSpanInfo(
        String spanId,
        String name,
        String httpMethod,
        String httpPath,
        Object requestBody,
        Object responseBody,
        Integer responseStatus
) {
}
