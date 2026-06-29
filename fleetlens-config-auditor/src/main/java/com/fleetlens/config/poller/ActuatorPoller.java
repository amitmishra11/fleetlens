package com.fleetlens.config.poller;

import com.fleetlens.common.event.ConfigDriftEvent;
import com.fleetlens.config.ServiceRegistryProperties;
import com.fleetlens.config.ServiceRegistryProperties.ServiceDefinitionProps;
import com.fleetlens.config.differ.ConfigDiff;
import com.fleetlens.config.differ.ConfigDiffEngine;
import com.fleetlens.config.store.ConfigSnapshot;
import com.fleetlens.config.store.ConfigSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class ActuatorPoller {

    private static final Logger log = LoggerFactory.getLogger(ActuatorPoller.class);

    private final RestTemplate restTemplate;
    private final ServiceRegistryProperties registry;
    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigDiffEngine diffEngine;
    private final ApplicationEventPublisher eventPublisher;

    public ActuatorPoller(RestTemplate restTemplate,
                           ServiceRegistryProperties registry,
                           ConfigSnapshotRepository snapshotRepo,
                           ConfigDiffEngine diffEngine,
                           ApplicationEventPublisher eventPublisher) {
        this.restTemplate = restTemplate;
        this.registry = registry;
        this.snapshotRepo = snapshotRepo;
        this.diffEngine = diffEngine;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${fleetlens.config.poll-interval-ms:60000}")
    public void poll() {
        registry.getServices().forEach(this::pollService);
    }

    private void pollService(ServiceDefinitionProps svc) {
        try {
            Map<String, Object> rawEnv = fetchActuatorEnv(svc);
            Map<String, Object> env = ActuatorEnvExtractor.extractProperties(rawEnv);
            Optional<ConfigSnapshot> prev = snapshotRepo.findLatest(svc.getId(), svc.getEnv());

            if (prev.isPresent()) {
                ConfigDiff diff = diffEngine.diff(prev.get().getConfigJson(), env);
                if (diff.hasChanges()) {
                    eventPublisher.publishEvent(new ConfigDriftEvent(svc.getId(), diff.getChangedKeys()));
                    log.warn("Config drift detected in service {}: {} keys changed",
                            svc.getId(), diff.getChangedKeys().size());
                }
            }
            snapshotRepo.save(new ConfigSnapshot(svc.getId(), svc.getEnv(), env));
        } catch (Exception e) {
            log.error("Failed to poll actuator for service {}", svc.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchActuatorEnv(ServiceDefinitionProps svc) {
        String url = svc.getBaseUrl() + "/actuator/env";
        Map<String, Object> body = restTemplate.getForObject(url, Map.class);
        return body == null ? Map.of() : body;
    }
}
