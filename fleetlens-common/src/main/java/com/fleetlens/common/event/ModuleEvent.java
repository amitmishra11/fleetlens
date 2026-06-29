package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public abstract class ModuleEvent {

    private final UUID id = UUID.randomUUID();
    private final String serviceId;
    private final ModuleType module;
    private final IncidentSeverity severity;
    private final Instant occurredAt;

    protected ModuleEvent(String serviceId, ModuleType module, IncidentSeverity severity, Instant occurredAt) {
        this.serviceId = serviceId;
        this.module = module;
        this.severity = severity;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public ModuleType getModule() { return module; }
    public IncidentSeverity getSeverity() { return severity; }
    public Instant getOccurredAt() { return occurredAt; }

    public abstract String getSummary();
    public abstract Map<String, Object> getPayload();
}
