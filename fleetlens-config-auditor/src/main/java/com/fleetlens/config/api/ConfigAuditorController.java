package com.fleetlens.config.api;

import com.fleetlens.common.util.JsonUtils;
import com.fleetlens.common.util.TimeUtils;
import com.fleetlens.config.ServiceRegistryProperties;
import com.fleetlens.config.ServiceRegistryProperties.ServiceDefinitionProps;
import com.fleetlens.config.differ.ConfigChange;
import com.fleetlens.config.differ.ConfigDiff;
import com.fleetlens.config.differ.ConfigDiffEngine;
import com.fleetlens.config.store.ConfigSnapshot;
import com.fleetlens.config.store.ConfigSnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigAuditorController {

    private final ServiceRegistryProperties registry;
    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigDiffEngine diffEngine;

    public ConfigAuditorController(ServiceRegistryProperties registry,
                                    ConfigSnapshotRepository snapshotRepo,
                                    ConfigDiffEngine diffEngine) {
        this.registry = registry;
        this.snapshotRepo = snapshotRepo;
        this.diffEngine = diffEngine;
    }

    @GetMapping("/services")
    public List<ServiceSummaryResponse> listServices() {
        return registry.getServices().stream()
                .map(svc -> new ServiceSummaryResponse(svc.getId(), svc.getBaseUrl(), svc.getEnv()))
                .collect(Collectors.toList());
    }

    @GetMapping("/services/{serviceId}/latest")
    public ResponseEntity<SnapshotResponse> latestSnapshot(@PathVariable String serviceId,
                                                            @RequestParam(required = false) String env) {
        Optional<ConfigSnapshot> snapshot = env != null
                ? snapshotRepo.findLatest(serviceId, env)
                : snapshotRepo.findLatestAnyEnv(serviceId);

        return snapshot.map(this::toSnapshotResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/services/{serviceId}/diff")
    public ResponseEntity<List<ConfigChange>> serviceDiff(@PathVariable String serviceId,
                                                           @RequestParam(required = false) String env) {
        List<ConfigSnapshot> snapshots = env != null
                ? snapshotRepo.findAllByServiceIdAndEnvOrderByCapturedAtDesc(serviceId, env)
                : snapshotRepo.findAllByServiceIdOrderByCapturedAtDesc(serviceId);

        if (snapshots.size() < 2) {
            return ResponseEntity.ok(List.of());
        }

        ConfigSnapshot latest = snapshots.get(0);
        ConfigSnapshot previous = snapshots.get(1);
        ConfigDiff diff = diffEngine.diff(previous.getConfigJson(), latest.getConfigJson());
        return ResponseEntity.ok(diff.changes());
    }

    @GetMapping("/drift")
    public List<DriftEventResponse> driftSince(@RequestParam String since) {
        Instant sinceInstant = TimeUtils.parseIsoOrNow(since);
        List<DriftEventResponse> results = new ArrayList<>();

        for (ServiceDefinitionProps svc : registry.getServices()) {
            List<ConfigSnapshot> history = snapshotRepo
                    .findAllByServiceIdAndEnvOrderByCapturedAtDesc(svc.getId(), svc.getEnv());
            history.sort(Comparator.comparing(ConfigSnapshot::getCapturedAt));

            for (int i = 1; i < history.size(); i++) {
                ConfigSnapshot prev = history.get(i - 1);
                ConfigSnapshot curr = history.get(i);
                if (curr.getCapturedAt().isBefore(sinceInstant)) {
                    continue;
                }
                ConfigDiff diff = diffEngine.diff(prev.getConfigJson(), curr.getConfigJson());
                if (diff.hasChanges()) {
                    results.add(new DriftEventResponse(
                            svc.getId(), svc.getEnv(), curr.getCapturedAt(), diff.getChangedKeys()));
                }
            }
        }

        results.sort(Comparator.comparing(DriftEventResponse::occurredAt));
        return results;
    }

    @GetMapping("/matrix")
    public ConfigMatrixResponse matrix() {
        Set<String> environments = new LinkedHashSet<>();
        Map<String, Map<String, Object>> flattenedByEnv = new LinkedHashMap<>();

        for (ServiceDefinitionProps svc : registry.getServices()) {
            Optional<ConfigSnapshot> latest = snapshotRepo.findLatest(svc.getId(), svc.getEnv());
            if (latest.isEmpty()) {
                continue;
            }
            environments.add(svc.getEnv());
            Map<String, Object> flattened = JsonUtils.flatten(latest.get().getConfigJson());
            flattenedByEnv.merge(svc.getEnv(), flattened, (existing, incoming) -> {
                Map<String, Object> merged = new LinkedHashMap<>(existing);
                merged.putAll(incoming);
                return merged;
            });
        }

        Set<String> allKeys = new HashSet<>();
        flattenedByEnv.values().forEach(m -> allKeys.addAll(m.keySet()));

        List<ConfigMatrixResponse.MatrixRow> rows = allKeys.stream()
                .sorted()
                .map(key -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (String env : environments) {
                        values.put(env, flattenedByEnv.getOrDefault(env, Map.of()).get(key));
                    }
                    boolean hasDrift = values.values().stream().distinct().count() > 1;
                    return new ConfigMatrixResponse.MatrixRow(key, values, hasDrift);
                })
                .collect(Collectors.toList());

        return new ConfigMatrixResponse(new ArrayList<>(environments), rows);
    }

    private SnapshotResponse toSnapshotResponse(ConfigSnapshot snapshot) {
        return new SnapshotResponse(
                snapshot.getId(), snapshot.getServiceId(), snapshot.getEnv(),
                snapshot.getCapturedAt(), snapshot.getConfigJson());
    }
}
