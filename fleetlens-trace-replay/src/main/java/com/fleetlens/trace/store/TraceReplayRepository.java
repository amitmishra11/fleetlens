package com.fleetlens.trace.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TraceReplayRepository extends JpaRepository<TraceReplay, UUID> {
}
