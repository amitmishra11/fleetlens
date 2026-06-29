package com.fleetlens.memory.kafka;

/**
 * Fired after a consumer group's lag has been polled and persisted, so
 * HeapLagCorrelator can react via @EventListener. Intra-module only — not
 * part of fleetlens-common since no other module needs it.
 */
public class LagPollCompleteEvent {

    private final String serviceId;
    private final String consumerGroup;
    private final long totalLag;

    public LagPollCompleteEvent(String serviceId, String consumerGroup, long totalLag) {
        this.serviceId = serviceId;
        this.consumerGroup = consumerGroup;
        this.totalLag = totalLag;
    }

    public String getServiceId() { return serviceId; }
    public String getConsumerGroup() { return consumerGroup; }
    public long getTotalLag() { return totalLag; }
}
