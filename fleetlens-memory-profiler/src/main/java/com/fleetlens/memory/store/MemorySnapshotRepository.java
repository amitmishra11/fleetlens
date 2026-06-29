package com.fleetlens.memory.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MemorySnapshotRepository extends JpaRepository<MemorySnapshot, java.util.UUID> {

    default MemorySnapshot saveHeap(String serviceId, double heapUsedMb, double heapMaxMb, double gcPauseMs) {
        return save(MemorySnapshot.heap(serviceId, heapUsedMb, heapMaxMb, gcPauseMs));
    }

    default MemorySnapshot saveLag(String consumerGroup, String serviceId, long lag) {
        return save(MemorySnapshot.lag(consumerGroup, serviceId, lag));
    }

    @Query("select m from MemorySnapshot m where m.serviceId = :serviceId and m.heapUsedMb is not null " +
           "and m.sampledAt >= :since order by m.sampledAt asc")
    List<MemorySnapshot> findHeapSince(@Param("serviceId") String serviceId, @Param("since") Instant since);

    @Query("select m from MemorySnapshot m where m.serviceId = :serviceId and m.kafkaLag is not null " +
           "and m.sampledAt >= :since order by m.sampledAt asc")
    List<MemorySnapshot> findLagSince(@Param("serviceId") String serviceId, @Param("since") Instant since);

    @Query("select m from MemorySnapshot m where m.serviceId = :serviceId and m.heapUsedMb is not null " +
           "and m.sampledAt between :from and :to order by m.sampledAt asc")
    List<MemorySnapshot> findHeapBetween(@Param("serviceId") String serviceId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select m from MemorySnapshot m where m.serviceId = :serviceId and m.kafkaLag is not null " +
           "and m.sampledAt between :from and :to order by m.sampledAt asc")
    List<MemorySnapshot> findLagBetween(@Param("serviceId") String serviceId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select m from MemorySnapshot m where m.serviceId = :serviceId order by m.sampledAt desc limit 1")
    Optional<MemorySnapshot> findLatestForService(@Param("serviceId") String serviceId);

    @Query("select coalesce(sum(m.heapUsedMb), 0) from MemorySnapshot m where m.id in " +
           "(select m2.id from MemorySnapshot m2 where m2.heapUsedMb is not null and m2.sampledAt = " +
           "(select max(m3.sampledAt) from MemorySnapshot m3 where m3.serviceId = m2.serviceId and m3.heapUsedMb is not null))")
    Double findLatestHeapGlobal();

    @Query("select coalesce(sum(m.kafkaLag), 0) from MemorySnapshot m where m.id in " +
           "(select m2.id from MemorySnapshot m2 where m2.kafkaLag is not null and m2.sampledAt = " +
           "(select max(m3.sampledAt) from MemorySnapshot m3 where m3.consumerGroup = m2.consumerGroup and m3.kafkaLag is not null))")
    Long findLatestLagGlobal();
}
