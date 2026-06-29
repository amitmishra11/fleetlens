package com.fleetlens.correlation.timeline;

import com.fleetlens.correlation.incident.Incident;

import java.time.Instant;
import java.util.List;

public record ServiceTimeline(
    String serviceId,
    Instant from,
    Instant to,
    List<TimelineEntry> entries,
    List<Incident> incidents
) {
}
