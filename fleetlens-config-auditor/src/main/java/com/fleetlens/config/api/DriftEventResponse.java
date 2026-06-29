package com.fleetlens.config.api;

import java.time.Instant;
import java.util.List;

public record DriftEventResponse(String serviceId, String env, Instant occurredAt, List<String> changedKeys) {
}
