package com.fleetlens.trace.capture;

public record DownstreamCall(
        String spanId,
        String calledService,
        String httpMethod,
        String httpPath,
        Object requestCapture,
        Object responseCapture
) {
}
