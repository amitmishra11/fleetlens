package com.fleetlens.correlation.timeline;

import java.time.Instant;
import java.util.UUID;

public record TimelineEntry(
    Instant occurredAt,
    String module,
    String severity,
    String summary,
    UUID incidentId,
    String serviceId
) {
}
