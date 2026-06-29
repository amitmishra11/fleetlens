package com.fleetlens.correlation.incident;

import com.fleetlens.common.event.ModuleEvent;
import com.fleetlens.common.util.JsonUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "module_events")
public class ModuleEventEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false, length = 120)
    private String serviceId;

    @Column(name = "module", nullable = false, length = 40)
    private String module;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ModuleEventEntity() {
    }

    private ModuleEventEntity(UUID id, String serviceId, String module, String severity,
                               String summary, String payload, Instant occurredAt, Instant createdAt) {
        this.id = id;
        this.serviceId = serviceId;
        this.module = module;
        this.severity = severity;
        this.summary = summary;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static ModuleEventEntity from(ModuleEvent event) {
        return new ModuleEventEntity(
            event.getId(),
            event.getServiceId(),
            event.getModule().name(),
            event.getSeverity().name(),
            event.getSummary(),
            JsonUtils.toJson(event.getPayload()),
            event.getOccurredAt(),
            Instant.now()
        );
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public String getModule() { return module; }
    public String getSeverity() { return severity; }
    public String getSummary() { return summary; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
