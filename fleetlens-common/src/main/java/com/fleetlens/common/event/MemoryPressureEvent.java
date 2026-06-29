package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.Map;

public class MemoryPressureEvent extends ModuleEvent {

    private final double usedPercent;

    public MemoryPressureEvent(String serviceId, double usedPercent) {
        super(serviceId, ModuleType.MEMORY, IncidentSeverity.CRITICAL, Instant.now());
        this.usedPercent = usedPercent;
    }

    public double getUsedPercent() { return usedPercent; }

    @Override
    public String getSummary() {
        return "Heap usage at %.1f%%".formatted(usedPercent);
    }

    @Override
    public Map<String, Object> getPayload() {
        return Map.of("usedPercent", usedPercent);
    }
}
