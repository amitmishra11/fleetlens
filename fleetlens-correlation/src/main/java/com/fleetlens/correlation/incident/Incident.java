package com.fleetlens.correlation.incident;

import com.fleetlens.common.model.IncidentSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false, length = 120)
    private String serviceId;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * Postgres native UUID[] requires dialect-specific array handling that varies across
     * Hibernate versions. A comma-joined VARCHAR column converted via {@link UuidListConverter}
     * is simpler, fully portable, and avoids native array driver quirks - the safest choice here.
     */
    @Convert(converter = UuidListConverter.class)
    @Column(name = "module_event_ids", nullable = false, columnDefinition = "text")
    private List<UUID> moduleEventIds = new ArrayList<>();

    @Column(name = "correlation_score")
    private Double correlationScore;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Incident() {
    }

    public Incident(String serviceId, String title, IncidentSeverity severity,
                     List<UUID> moduleEventIds, double correlationScore,
                     Instant openedAt, Instant resolvedAt) {
        this.id = UUID.randomUUID();
        this.serviceId = serviceId;
        this.title = title;
        this.severity = severity.name();
        this.moduleEventIds = new ArrayList<>(moduleEventIds);
        this.correlationScore = correlationScore;
        this.openedAt = openedAt;
        this.resolvedAt = resolvedAt;
        this.createdAt = Instant.now();
    }

    /**
     * Appends the given event's id, escalates severity to the higher of current/new
     * (never downgrades), and recomputes correlation_score with a simple heuristic:
     * min(1.0, 0.5 + 0.1 * eventCount).
     */
    public void addEvent(ModuleEventEntity event) {
        moduleEventIds.add(event.getId());

        IncidentSeverity current = IncidentSeverity.valueOf(this.severity);
        IncidentSeverity incoming = IncidentSeverity.valueOf(event.getSeverity());
        if (incoming.isHigherThan(current)) {
            this.severity = incoming.name();
        }

        this.correlationScore = Math.min(1.0, 0.5 + 0.1 * getEventCount());
    }

    public int getEventCount() {
        return moduleEventIds.size();
    }

    public void resolve() {
        this.resolvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public String getTitle() { return title; }
    public IncidentSeverity getSeverity() { return IncidentSeverity.valueOf(severity); }
    public List<UUID> getEventIds() { return moduleEventIds; }
    public Double getCorrelationScore() { return correlationScore; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
