package com.fleetlens.trace.store;

import com.fleetlens.trace.capture.ReplayBundle;
import com.fleetlens.trace.capture.TraceSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trace_replays")
public class TraceReplay {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false, length = 120)
    private String serviceId;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Lob
    @Column(name = "replay_file", nullable = false)
    private byte[] replayFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_diff")
    private String lastDiff;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TraceReplay() {
        // JPA
    }

    public TraceReplay(UUID id, String serviceId, String traceId, byte[] replayFile, Instant recordedAt) {
        this.id = id;
        this.serviceId = serviceId;
        this.traceId = traceId;
        this.replayFile = replayFile;
        this.recordedAt = recordedAt;
        this.createdAt = Instant.now();
    }

    public static TraceReplay fromBundle(ReplayBundle bundle) {
        UUID id = bundle.replayId() != null ? UUID.fromString(bundle.replayId()) : UUID.randomUUID();
        Instant recordedAt = bundle.recordedAt() != null ? bundle.recordedAt() : Instant.now();
        return new TraceReplay(id, bundle.serviceId(), bundle.traceId(), TraceSerializer.toBytes(bundle), recordedAt);
    }

    public ReplayBundle toBundle() {
        return TraceSerializer.fromBytes(replayFile);
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public String getTraceId() { return traceId; }
    public byte[] getReplayFile() { return replayFile; }
    public String getLastDiff() { return lastDiff; }
    public void setLastDiff(String lastDiff) { this.lastDiff = lastDiff; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
