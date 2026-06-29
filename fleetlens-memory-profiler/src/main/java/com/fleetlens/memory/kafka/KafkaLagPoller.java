package com.fleetlens.memory.kafka;

import com.fleetlens.common.registry.ServiceDefinition;
import com.fleetlens.common.registry.ServiceDirectory;
import com.fleetlens.memory.store.MemorySnapshotRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaLagPoller {

    private static final Logger log = LoggerFactory.getLogger(KafkaLagPoller.class);
    private static final long TIMEOUT_SECONDS = 10;

    private final AdminClient adminClient;
    private final ServiceDirectory registry;
    private final MemorySnapshotRepository snapshotRepo;
    private final ApplicationEventPublisher eventPublisher;

    public KafkaLagPoller(@Qualifier("kafkaAdminClient") AdminClient adminClient, ServiceDirectory registry,
                           MemorySnapshotRepository snapshotRepo, ApplicationEventPublisher eventPublisher) {
        this.adminClient = adminClient;
        this.registry = registry;
        this.snapshotRepo = snapshotRepo;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${fleetlens.memory.poll-interval-ms:30000}")
    public void poll() {
        for (ServiceDefinition svc : registry.list()) {
            for (String groupId : svc.kafkaConsumerGroups()) {
                try {
                    pollGroup(svc.id(), groupId);
                } catch (Exception e) {
                    log.error("Kafka lag poll failed for service {} group {}", svc.id(), groupId, e);
                }
            }
        }
    }

    private void pollGroup(String serviceId, String groupId) throws Exception {
        Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committed = adminClient
                .listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (committed.isEmpty()) {
            log.debug("No committed offsets found for group {}", groupId);
            return;
        }

        Map<TopicPartition, OffsetSpec> latestSpecs = new java.util.HashMap<>();
        committed.keySet().forEach(tp -> latestSpecs.put(tp, OffsetSpec.latest()));

        ListOffsetsResult latestResult = adminClient.listOffsets(latestSpecs);

        long totalLag = 0L;
        for (Map.Entry<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> entry : committed.entrySet()) {
            TopicPartition tp = entry.getKey();
            long committedOffset = entry.getValue().offset();
            long endOffset = latestResult.partitionResult(tp).get(TIMEOUT_SECONDS, TimeUnit.SECONDS).offset();
            totalLag += Math.max(0, endOffset - committedOffset);
        }

        snapshotRepo.saveLag(groupId, serviceId, totalLag);
        eventPublisher.publishEvent(new LagPollCompleteEvent(serviceId, groupId, totalLag));
    }
}
