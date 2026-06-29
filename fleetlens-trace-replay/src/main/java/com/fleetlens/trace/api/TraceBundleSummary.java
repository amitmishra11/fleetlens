package com.fleetlens.trace.api;

import java.time.Instant;
import java.util.UUID;

public record TraceBundleSummary(UUID replayId, String traceId, String serviceId, Instant recordedAt) {
}
