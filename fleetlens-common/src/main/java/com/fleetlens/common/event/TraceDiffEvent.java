package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.Map;

public class TraceDiffEvent extends ModuleEvent {

    private final String traceId;
    private final int diffCount;

    public TraceDiffEvent(String serviceId, String traceId, int diffCount) {
        super(serviceId, ModuleType.TRACE_REPLAY,
            diffCount > 0 ? IncidentSeverity.WARN : IncidentSeverity.INFO, Instant.now());
        this.traceId = traceId;
        this.diffCount = diffCount;
    }

    public String getTraceId() { return traceId; }
    public int getDiffCount() { return diffCount; }

    @Override
    public String getSummary() {
        return "Trace replay diff for " + traceId + ": " + diffCount + " field(s) changed";
    }

    @Override
    public Map<String, Object> getPayload() {
        return Map.of("traceId", traceId, "diffCount", diffCount);
    }
}
