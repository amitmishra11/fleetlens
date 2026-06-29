package com.fleetlens.correlation.engine;

import com.fleetlens.common.event.ConfigDriftEvent;
import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.correlation.incident.Incident;
import com.fleetlens.correlation.incident.IncidentBuilder;
import com.fleetlens.correlation.incident.IncidentRepository;
import com.fleetlens.correlation.incident.ModuleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationEngineTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentBuilder incidentBuilder;

    @Mock
    private ModuleEventRepository moduleEventRepository;

    private CorrelationEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        engine = new CorrelationEngine(incidentRepository, incidentBuilder, moduleEventRepository);
        Field windowField = CorrelationEngine.class.getDeclaredField("correlationWindowMinutes");
        windowField.setAccessible(true);
        windowField.set(engine, 5);
    }

    @Test
    void eventWithinWindow_mergesIntoExistingIncident() {
        Incident existing = new Incident("svc-1", "existing", IncidentSeverity.WARN,
            List.of(UUID.randomUUID()), 0.5, Instant.now().minusSeconds(60), null);

        when(incidentRepository.findOpenForService(any(), any())).thenReturn(List.of(existing));

        ConfigDriftEvent event = new ConfigDriftEvent("svc-1", List.of("key.a"));
        engine.onConfigDrift(event);

        verify(incidentRepository).save(existing);
        verify(incidentBuilder, never()).createFrom(any());
    }

    @Test
    void eventOutsideWindow_createsNewIncident() {
        when(incidentRepository.findOpenForService(any(), any())).thenReturn(List.of());

        Incident created = new Incident("svc-1", "new", IncidentSeverity.WARN,
            List.of(UUID.randomUUID()), 0.5, Instant.now(), null);
        when(incidentBuilder.createFrom(any())).thenReturn(created);

        ConfigDriftEvent event = new ConfigDriftEvent("svc-1", List.of("key.a"));
        engine.onConfigDrift(event);

        verify(incidentBuilder).createFrom(event);
        verify(incidentRepository).save(created);
    }
}
