package com.fleetlens.correlation.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ModuleEventRepository extends JpaRepository<ModuleEventEntity, UUID> {

    List<ModuleEventEntity> findByServiceIdAndOccurredAtBetweenOrderByOccurredAtAsc(
        String serviceId, Instant from, Instant to);

    List<ModuleEventEntity> findByModuleAndOccurredAtAfter(String module, Instant after);
}
