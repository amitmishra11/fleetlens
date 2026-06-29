package com.fleetlens.trace.capture;

import java.util.Map;

public record CapturedSpan(
        String traceId,
        String spanId,
        String parentSpanId,
        String name,
        String kind,
        long startEpochNanos,
        long endEpochNanos,
        Map<String, Object> attributes,
        String statusCode
) {
}
