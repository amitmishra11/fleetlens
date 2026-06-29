package com.fleetlens.memory.correlator;

import com.fleetlens.common.event.HeapLagCorrelationEvent;
import com.fleetlens.memory.kafka.LagPollCompleteEvent;
import com.fleetlens.memory.store.MemorySnapshot;
import com.fleetlens.memory.store.MemorySnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class HeapLagCorrelator {

    private static final Logger log = LoggerFactory.getLogger(HeapLagCorrelator.class);
    private static final double TREND_THRESHOLD = 0.05;
    private static final int MIN_SAMPLES = 3;
    private static final long WINDOW_SECONDS = 300;

    private final MemorySnapshotRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    private final CorrelationLog correlationLog;

    public HeapLagCorrelator(MemorySnapshotRepository repo, ApplicationEventPublisher eventPublisher,
                              CorrelationLog correlationLog) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
        this.correlationLog = correlationLog;
    }

    @EventListener(LagPollCompleteEvent.class)
    public void correlate(LagPollCompleteEvent event) {
        String serviceId = event.getServiceId();
        Instant windowStart = Instant.now().minusSeconds(WINDOW_SECONDS);

        List<MemorySnapshot> heapSamples = repo.findHeapSince(serviceId, windowStart);
        List<MemorySnapshot> lagSamples = repo.findLagSince(serviceId, windowStart);

        if (heapSamples.size() < MIN_SAMPLES || lagSamples.size() < MIN_SAMPLES) {
            return;
        }

        double heapTrend = TrendCalculator.linearTrend(
                heapSamples.stream().map(s -> s.getHeapUsedMb().doubleValue()).toList());
        double lagTrend = TrendCalculator.linearTrend(
                lagSamples.stream().map(s -> s.getKafkaLag().doubleValue()).toList());

        if (heapTrend > TREND_THRESHOLD && lagTrend > TREND_THRESHOLD) {
            HeapLagCorrelationEvent correlationEvent = new HeapLagCorrelationEvent(
                    serviceId, heapTrend, lagTrend,
                    "Heap growth correlates with rising consumer lag");
            eventPublisher.publishEvent(correlationEvent);
            correlationLog.record(correlationEvent);
            log.warn("Heap-lag correlation detected for {}: heapTrend={}, lagTrend={}", serviceId, heapTrend, lagTrend);
        }
    }
}
