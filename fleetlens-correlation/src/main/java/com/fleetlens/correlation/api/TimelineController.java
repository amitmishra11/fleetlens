package com.fleetlens.correlation.api;

import com.fleetlens.common.util.TimeUtils;
import com.fleetlens.correlation.incident.Incident;
import com.fleetlens.correlation.incident.IncidentRepository;
import com.fleetlens.correlation.incident.ModuleEventEntity;
import com.fleetlens.correlation.incident.ModuleEventRepository;
import com.fleetlens.correlation.timeline.ServiceTimeline;
import com.fleetlens.correlation.timeline.TimelineAssembler;
import com.fleetlens.correlation.timeline.TimelineEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineController {

    private static final long DEFAULT_RANGE_MINUTES = 60;

    private final TimelineAssembler timelineAssembler;
    private final ModuleEventRepository moduleEventRepository;
    private final IncidentRepository incidentRepository;

    public TimelineController(TimelineAssembler timelineAssembler, ModuleEventRepository moduleEventRepository,
                               IncidentRepository incidentRepository) {
        this.timelineAssembler = timelineAssembler;
        this.moduleEventRepository = moduleEventRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/{serviceId}")
    public ServiceTimeline serviceTimeline(@PathVariable String serviceId,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        Instant toInstant = parseOrNow(to);
        Instant fromInstant = parseOrDefault(from, toInstant.minusSeconds(DEFAULT_RANGE_MINUTES * 60));
        return timelineAssembler.assemble(serviceId, fromInstant, toInstant);
    }

    @GetMapping("/global")
    public List<TimelineEntry> globalTimeline(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to) {
        Instant toInstant = parseOrNow(to);
        Instant fromInstant = parseOrDefault(from, toInstant.minusSeconds(DEFAULT_RANGE_MINUTES * 60));

        List<ModuleEventEntity> events = moduleEventRepository.findAll().stream()
            .filter(e -> !e.getOccurredAt().isBefore(fromInstant) && !e.getOccurredAt().isAfter(toInstant))
            .toList();
        List<Incident> incidents = incidentRepository.findByOpenedAtBetween(fromInstant, toInstant);

        return events.stream()
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
    }

    private UUID findIncidentId(List<Incident> incidents, ModuleEventEntity event) {
        return incidents.stream()
            .filter(i -> i.getEventIds().contains(event.getId()))
            .map(Incident::getId)
            .findFirst()
            .orElse(null);
    }

    private Instant parseOrNow(String iso) {
        return iso == null || iso.isBlank() ? Instant.now() : TimeUtils.parseIsoOrNow(iso);
    }

    private Instant parseOrDefault(String iso, Instant fallback) {
        return iso == null || iso.isBlank() ? fallback : TimeUtils.parseIsoOrNow(iso);
    }
}
