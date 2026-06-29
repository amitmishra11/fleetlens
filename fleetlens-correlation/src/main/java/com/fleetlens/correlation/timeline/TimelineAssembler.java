package com.fleetlens.correlation.timeline;

import com.fleetlens.correlation.incident.Incident;
import com.fleetlens.correlation.incident.ModuleEventEntity;
import com.fleetlens.correlation.incident.ModuleEventRepository;
import com.fleetlens.correlation.incident.IncidentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class TimelineAssembler {

    private final ModuleEventRepository moduleEventRepository;
    private final IncidentRepository incidentRepository;

    public TimelineAssembler(ModuleEventRepository moduleEventRepository, IncidentRepository incidentRepository) {
        this.moduleEventRepository = moduleEventRepository;
        this.incidentRepository = incidentRepository;
    }

    public ServiceTimeline assemble(String serviceId, Instant from, Instant to) {
        List<ModuleEventEntity> events = moduleEventRepository
            .findByServiceIdAndOccurredAtBetweenOrderByOccurredAtAsc(serviceId, from, to);
        List<Incident> incidents = incidentRepository.findByServiceIdAndRange(serviceId, from, to);

        List<TimelineEntry> entries = events.stream()
            .map(e -> new TimelineEntry(
                e.getOccurredAt(),
                e.getModule(),
                e.getSeverity(),
                e.getSummary(),
                findIncidentId(incidents, e),
                e.getServiceId()
            ))
            .sorted(Comparator.comparing(TimelineEntry::occurredAt))
            .toList();

        return new ServiceTimeline(serviceId, from, to, entries, incidents);
    }

    private java.util.UUID findIncidentId(List<Incident> incidents, ModuleEventEntity event) {
        return incidents.stream()
            .filter(i -> i.getEventIds().contains(event.getId()))
            .map(Incident::getId)
            .findFirst()
            .orElse(null);
    }
}
