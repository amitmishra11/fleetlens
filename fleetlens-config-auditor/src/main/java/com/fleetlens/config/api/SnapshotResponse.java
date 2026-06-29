package com.fleetlens.config.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SnapshotResponse(UUID id, String serviceId, String env, Instant capturedAt,
                                Map<String, Object> configJson) {
}
