package com.fleetlens.common.registry;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for which services FleetLens monitors, shared across
 * all modules. Seeded at startup from static config (fleetlens.services in
 * application.yml) and mutable at runtime via the config-auditor API, so a
 * locally-running service can be registered for monitoring without a restart.
 */
@Component
public class ServiceDirectory {

    private final Map<String, ServiceDefinition> services = new ConcurrentHashMap<>();

    public void register(ServiceDefinition definition) {
        services.put(definition.id(), definition);
    }

    public boolean unregister(String id) {
        ServiceDefinition existing = services.get(id);
        if (existing == null || !existing.managed()) {
            return false;
        }
        return services.remove(id) != null;
    }

    public List<ServiceDefinition> list() {
        return List.copyOf(services.values());
    }

    public Optional<ServiceDefinition> find(String id) {
        return Optional.ofNullable(services.get(id));
    }
}
