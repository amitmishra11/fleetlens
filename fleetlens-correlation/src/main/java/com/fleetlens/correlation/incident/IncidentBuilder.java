package com.fleetlens.correlation.incident;

import com.fleetlens.common.event.ModuleEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentBuilder {

    private final ModuleEventRepository moduleEventRepository;

    public IncidentBuilder(ModuleEventRepository moduleEventRepository) {
        this.moduleEventRepository = moduleEventRepository;
    }

    /**
     * Persists the triggering event and builds a brand-new Incident around it.
     * The caller (CorrelationEngine) is responsible for saving the returned Incident.
     */
    public Incident createFrom(ModuleEvent event) {
        ModuleEventEntity entity = ModuleEventEntity.from(event);
        moduleEventRepository.save(entity);

        String title = event.getModule() + " incident: " + event.getSummary();

        return new Incident(
            event.getServiceId(),
            title,
            event.getSeverity(),
            List.of(entity.getId()),
            0.5,
            event.getOccurredAt(),
            null
        );
    }
}
