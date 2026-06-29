package com.fleetlens.config.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigSnapshotRepository extends JpaRepository<ConfigSnapshot, UUID> {

    @Query("select s from ConfigSnapshot s where s.serviceId = :serviceId and s.env = :env "
            + "order by s.capturedAt desc")
    List<ConfigSnapshot> findAllByServiceIdAndEnvOrderByCapturedAtDesc(
            @Param("serviceId") String serviceId, @Param("env") String env);

    default Optional<ConfigSnapshot> findLatest(String serviceId, String env) {
        List<ConfigSnapshot> results = findAllByServiceIdAndEnvOrderByCapturedAtDesc(serviceId, env);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("select s from ConfigSnapshot s where s.serviceId = :serviceId order by s.capturedAt desc")
    List<ConfigSnapshot> findAllByServiceIdOrderByCapturedAtDesc(@Param("serviceId") String serviceId);

    default Optional<ConfigSnapshot> findLatestAnyEnv(String serviceId) {
        List<ConfigSnapshot> results = findAllByServiceIdOrderByCapturedAtDesc(serviceId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    List<ConfigSnapshot> findByServiceIdAndCapturedAtAfterOrderByCapturedAtAsc(String serviceId, Instant since);

    List<ConfigSnapshot> findByCapturedAtAfterOrderByServiceIdAscCapturedAtAsc(Instant since);

    List<ConfigSnapshot> findByEnvOrderByCapturedAtDesc(String env);
}
