package com.fleetlens.memory.correlator;

import com.fleetlens.memory.store.MemorySnapshotRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FleetLensMetrics {

    private final Counter incidentsTotal;

    public FleetLensMetrics(MeterRegistry registry, MemorySnapshotRepository repo) {
        Gauge.builder("fleetlens.heap.used_mb", repo, r -> {
                    Double total = r.findLatestHeapGlobal();
                    return total == null ? 0.0 : total;
                })
                .description("Total heap used across all monitored services")
                .register(registry);

        Gauge.builder("fleetlens.kafka.total_lag", repo, r -> {
                    Long total = r.findLatestLagGlobal();
                    return total == null ? 0.0 : total.doubleValue();
                })
                .description("Total Kafka consumer lag across all groups")
                .register(registry);

        // Registered here so the gauge/counter exists from module startup; the
        // correlation engine module (fleetlens-correlation) increments this
        // counter whenever it builds an incident from a module event.
        this.incidentsTotal = Counter.builder("fleetlens.incidents.total")
                .description("Total incidents raised")
                .register(registry);
    }

    public Counter incidentsTotal() {
        return incidentsTotal;
    }
}
