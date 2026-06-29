package com.fleetlens.schema.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, UUID> {

    Optional<SchemaVersion> findFirstByTopicOrderByVersionDesc(String topic);

    default Optional<SchemaVersion> findLatest(String topic) {
        return findFirstByTopicOrderByVersionDesc(topic);
    }

    List<SchemaVersion> findByTopicOrderByVersionAsc(String topic);

    List<SchemaVersion> findByBreakingTrueAndDetectedAtAfter(Instant since);

    @Query("select distinct s.topic from SchemaVersion s")
    List<String> findDistinctTopics();

    Optional<SchemaVersion> findByTopicAndVersion(String topic, int version);
}
