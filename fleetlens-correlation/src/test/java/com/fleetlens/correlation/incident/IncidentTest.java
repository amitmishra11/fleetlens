package com.fleetlens.correlation.incident;

import com.fleetlens.common.model.IncidentSeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentTest {

    private ModuleEventEntity eventWithSeverity(IncidentSeverity severity) {
        return ModuleEventEntity.from(new TestEvent("svc-1", severity));
    }

    @Test
    void addEvent_escalatesToHigherSeverity() {
        Incident incident = new Incident("svc-1", "title", IncidentSeverity.INFO,
            List.of(UUID.randomUUID()), 0.5, Instant.now(), null);

        incident.addEvent(eventWithSeverity(IncidentSeverity.CRITICAL));

        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    void addEvent_neverDowngradesSeverity() {
        Incident incident = new Incident("svc-1", "title", IncidentSeverity.CRITICAL,
            List.of(UUID.randomUUID()), 0.5, Instant.now(), null);

        incident.addEvent(eventWithSeverity(IncidentSeverity.INFO));

        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    void addEvent_appendsEventIdAndIncrementsCount() {
        Incident incident = new Incident("svc-1", "title", IncidentSeverity.WARN,
            List.of(UUID.randomUUID()), 0.5, Instant.now(), null);

        incident.addEvent(eventWithSeverity(IncidentSeverity.WARN));

        assertThat(incident.getEventCount()).isEqualTo(2);
    }

    private static class TestEvent extends com.fleetlens.common.event.ModuleEvent {
        TestEvent(String serviceId, IncidentSeverity severity) {
            super(serviceId, com.fleetlens.common.model.ModuleType.CONFIG, severity, Instant.now());
        }

        @Override
        public String getSummary() { return "test event"; }

        @Override
        public java.util.Map<String, Object> getPayload() { return java.util.Map.of(); }
    }
}
