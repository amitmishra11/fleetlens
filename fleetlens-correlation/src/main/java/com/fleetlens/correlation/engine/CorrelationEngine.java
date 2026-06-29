package com.fleetlens.correlation.engine;

import com.fleetlens.common.event.ConfigDriftEvent;
import com.fleetlens.common.event.HeapLagCorrelationEvent;
import com.fleetlens.common.event.MemoryPressureEvent;
import com.fleetlens.common.event.ModuleEvent;
import com.fleetlens.common.event.SchemaDriftEvent;
import com.fleetlens.common.event.TraceDiffEvent;
import com.fleetlens.correlation.incident.Incident;
import com.fleetlens.correlation.incident.IncidentBuilder;
import com.fleetlens.correlation.incident.IncidentRepository;
import com.fleetlens.correlation.incident.ModuleEventEntity;
import com.fleetlens.correlation.incident.ModuleEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Subscribes to all module event types and joins them into incidents by
 * serviceId + a sliding correlation window. See DESIGN.md for the algorithm.
 */
@Component
public class CorrelationEngine {

    private static final Logger log = LoggerFactory.getLogger(CorrelationEngine.class);

    private final IncidentRepository incidentRepo;
    private final IncidentBuilder incidentBuilder;
    private final ModuleEventRepository moduleEventRepo;

    @Value("${fleetlens.correlation.window-minutes:5}")
    private int correlationWindowMinutes;

    public CorrelationEngine(IncidentRepository incidentRepo, IncidentBuilder incidentBuilder,
                              ModuleEventRepository moduleEventRepo) {
        this.incidentRepo = incidentRepo;
        this.incidentBuilder = incidentBuilder;
        this.moduleEventRepo = moduleEventRepo;
    }

    @EventListener
    public void onSchemaDrift(SchemaDriftEvent e) { handle(e); }

    @EventListener
    public void onConfigDrift(ConfigDriftEvent e) { handle(e); }

    @EventListener
    public void onMemoryPressure(MemoryPressureEvent e) { handle(e); }

    @EventListener
    public void onHeapLagCorrelation(HeapLagCorrelationEvent e) { handle(e); }

    @EventListener
    public void onTraceDiff(TraceDiffEvent e) { handle(e); }

    private void handle(ModuleEvent event) {
        Instant windowStart = event.getOccurredAt().minus(correlationWindowMinutes, ChronoUnit.MINUTES);

        List<Incident> openIncidents = incidentRepo.findOpenForService(event.getServiceId(), windowStart);

        if (openIncidents.isEmpty()) {
            // New incident; IncidentBuilder already persists the triggering ModuleEventEntity.
            Incident incident = incidentBuilder.createFrom(event);
            incidentRepo.save(incident);
            log.info("New incident created: {} for service {}", incident.getId(), event.getServiceId());
        } else {
            // Correlate into existing incident (highest severity wins)
            Incident existing = openIncidents.get(0);
            ModuleEventEntity entity = ModuleEventEntity.from(event);
            moduleEventRepo.save(entity);
            existing.addEvent(entity);
            incidentRepo.save(existing);
            log.info("Event correlated into incident {}: {} -> now has {} signals",
                existing.getId(), event.getModule(), existing.getEventCount());
        }
    }
}
