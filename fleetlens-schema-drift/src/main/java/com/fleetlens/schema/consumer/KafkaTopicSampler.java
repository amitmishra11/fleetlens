package com.fleetlens.schema.consumer;

import com.fleetlens.schema.alert.DriftAlertPublisher;
import com.fleetlens.schema.detector.DriftAnalyser;
import com.fleetlens.schema.detector.DriftReport;
import com.fleetlens.schema.detector.InferredSchema;
import com.fleetlens.schema.detector.SchemaInferenceEngine;
import com.fleetlens.schema.store.SchemaVersion;
import com.fleetlens.schema.store.SchemaVersionRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Periodically lists Kafka topics, samples recent messages from each, infers a schema, and
 * compares it against the latest stored version, recording drift and publishing
 * SchemaDriftEvent for breaking changes.
 */
@Component
public class KafkaTopicSampler {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicSampler.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final int MAX_POLL_ATTEMPTS = 10;

    private final AdminClient adminClient;
    private final SchemaDriftKafkaConfig.SamplerConsumerFactory consumerFactory;
    private final SchemaInferenceEngine inferenceEngine;
    private final DriftAnalyser analyser;
    private final SchemaVersionRepository versionRepo;
    private final DriftAlertPublisher alertPublisher;

    @Value("${fleetlens.schema.sample-count:50}")
    private int sampleCount;

    public KafkaTopicSampler(@Qualifier("schemaDriftAdminClient") AdminClient adminClient,
                              SchemaDriftKafkaConfig.SamplerConsumerFactory consumerFactory,
                              SchemaInferenceEngine inferenceEngine,
                              DriftAnalyser analyser,
                              SchemaVersionRepository versionRepo,
                              DriftAlertPublisher alertPublisher) {
        this.adminClient = adminClient;
        this.consumerFactory = consumerFactory;
        this.inferenceEngine = inferenceEngine;
        this.analyser = analyser;
        this.versionRepo = versionRepo;
        this.alertPublisher = alertPublisher;
    }

    @Scheduled(fixedDelayString = "${fleetlens.schema.poll-interval-ms:120000}")
    public void sampleAllTopics() {
        List<String> topics;
        try {
            topics = adminClient.listTopics().names().get().stream()
                .filter(t -> !t.startsWith("_"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list Kafka topics", e);
            return;
        }
        topics.forEach(this::processTopic);
    }

    void processTopic(String topic) {
        try {
            List<String> samples = sample(topic, sampleCount);
            if (samples.isEmpty()) {
                return;
            }
            InferredSchema current = inferenceEngine.inferFromJsonSamples(topic, samples);
            Optional<SchemaVersion> prev = versionRepo.findLatest(topic);

            if (prev.isEmpty()) {
                versionRepo.save(SchemaVersion.initial(topic, current));
                return;
            }

            InferredSchema prevSchema = prev.get().toInferred();
            InferredSchema nextSchema = current.withVersion(prevSchema.version() + 1);
            DriftReport report = analyser.analyse(prevSchema, nextSchema);

            if (report.hasFindings()) {
                versionRepo.save(SchemaVersion.next(topic, nextSchema, report));
                alertPublisher.publishIfBreaking(topic, report);
            }
        } catch (Exception e) {
            log.error("Failed to process schema sampling for topic {}", topic, e);
        }
    }

    private List<String> sample(String topic, int maxMessages) {
        List<String> messages = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = consumerFactory.create()) {
            consumer.subscribe(List.of(topic));
            int attempts = 0;
            while (messages.size() < maxMessages && attempts < MAX_POLL_ATTEMPTS) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    attempts++;
                    continue;
                }
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() != null) {
                        messages.add(record.value());
                    }
                    if (messages.size() >= maxMessages) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sample topic {}: {}", topic, e.getMessage());
        }
        return messages;
    }
}
