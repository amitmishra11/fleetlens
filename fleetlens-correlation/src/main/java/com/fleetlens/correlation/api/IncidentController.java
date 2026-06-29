package com.fleetlens.correlation.api;

import com.fleetlens.correlation.incident.Incident;
import com.fleetlens.correlation.incident.IncidentRepository;
import com.fleetlens.correlation.incident.ModuleEventEntity;
import com.fleetlens.correlation.incident.ModuleEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final ModuleEventRepository moduleEventRepository;

    public IncidentController(IncidentRepository incidentRepository, ModuleEventRepository moduleEventRepository) {
        this.incidentRepository = incidentRepository;
        this.moduleEventRepository = moduleEventRepository;
    }

    @GetMapping
    public Page<Incident> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return incidentRepository.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentDetail> get(@PathVariable UUID incidentId) {
        return incidentRepository.findById(incidentId)
            .map(incident -> {
                List<ModuleEventEntity> events = moduleEventRepository.findAllById(incident.getEventIds());
                return ResponseEntity.ok(new IncidentDetail(incident, events));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{incidentId}/resolve")
    public ResponseEntity<Incident> resolve(@PathVariable UUID incidentId) {
        return incidentRepository.findById(incidentId)
            .map(incident -> {
                incident.resolve();
                return ResponseEntity.ok(incidentRepository.save(incident));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    public record IncidentDetail(Incident incident, List<ModuleEventEntity> events) {
    }
}
