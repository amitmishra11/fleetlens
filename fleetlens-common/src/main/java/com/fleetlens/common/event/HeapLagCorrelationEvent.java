package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.Map;

public class HeapLagCorrelationEvent extends ModuleEvent {

    private final double heapTrend;
    private final double lagTrend;
    private final String message;

    public HeapLagCorrelationEvent(String serviceId, double heapTrend, double lagTrend, String message) {
        super(serviceId, ModuleType.MEMORY, IncidentSeverity.WARN, Instant.now());
        this.heapTrend = heapTrend;
        this.lagTrend = lagTrend;
        this.message = message;
    }

    public double getHeapTrend() { return heapTrend; }
    public double getLagTrend() { return lagTrend; }

    @Override
    public String getSummary() { return message; }

    @Override
    public Map<String, Object> getPayload() {
        return Map.of("heapTrend", heapTrend, "lagTrend", lagTrend, "message", message);
    }
}
