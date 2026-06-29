package com.fleetlens.memory.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_snapshots")
public class MemorySnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "heap_used_mb")
    private BigDecimal heapUsedMb;

    @Column(name = "heap_max_mb")
    private BigDecimal heapMaxMb;

    @Column(name = "gc_pause_ms")
    private BigDecimal gcPauseMs;

    @Column(name = "kafka_lag")
    private Long kafkaLag;

    @Column(name = "consumer_group")
    private String consumerGroup;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;

    protected MemorySnapshot() {}

    public static MemorySnapshot heap(String serviceId, double heapUsedMb, double heapMaxMb, double gcPauseMs) {
        MemorySnapshot s = new MemorySnapshot();
        s.serviceId = serviceId;
        s.heapUsedMb = BigDecimal.valueOf(heapUsedMb);
        s.heapMaxMb = BigDecimal.valueOf(heapMaxMb);
        s.gcPauseMs = BigDecimal.valueOf(gcPauseMs);
        s.sampledAt = Instant.now();
        return s;
    }

    public static MemorySnapshot lag(String consumerGroup, String serviceId, long kafkaLag) {
        MemorySnapshot s = new MemorySnapshot();
        s.serviceId = serviceId;
        s.consumerGroup = consumerGroup;
        s.kafkaLag = kafkaLag;
        s.sampledAt = Instant.now();
        return s;
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public BigDecimal getHeapUsedMb() { return heapUsedMb; }
    public BigDecimal getHeapMaxMb() { return heapMaxMb; }
    public BigDecimal getGcPauseMs() { return gcPauseMs; }
    public Long getKafkaLag() { return kafkaLag; }
    public String getConsumerGroup() { return consumerGroup; }
    public Instant getSampledAt() { return sampledAt; }
}
