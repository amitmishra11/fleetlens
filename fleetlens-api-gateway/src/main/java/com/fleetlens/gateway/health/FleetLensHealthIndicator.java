package com.fleetlens.gateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FleetLensHealthIndicator implements HealthIndicator {

    private static final List<String> ENABLED_MODULES = List.of(
        "schema-drift", "trace-replay", "memory-profiler", "config-auditor", "correlation"
    );

    @Override
    public Health health() {
        return Health.up()
            .withDetail("moduleCount", ENABLED_MODULES.size())
            .withDetail("enabledModules", ENABLED_MODULES)
            .build();
    }
}
