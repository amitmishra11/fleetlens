package com.fleetlens.correlation.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    @Query("select i from Incident i where i.serviceId = :serviceId "
        + "and i.resolvedAt is null and i.openedAt >= :windowStart order by i.openedAt desc")
    List<Incident> findOpenForService(@Param("serviceId") String serviceId,
                                       @Param("windowStart") Instant windowStart);

    @Query("select i from Incident i where i.serviceId = :serviceId "
        + "and i.openedAt >= :from and i.openedAt <= :to order by i.openedAt asc")
    List<Incident> findByServiceIdAndRange(@Param("serviceId") String serviceId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    Page<Incident> findAll(Pageable pageable);

    List<Incident> findByOpenedAtBetween(Instant from, Instant to);
}
