package com.fleetlens.config.store;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "config_snapshots")
public class ConfigSnapshot {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "env", nullable = false)
    private String env;

    @Convert(converter = JsonMapConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false)
    private Map<String, Object> configJson = new LinkedHashMap<>();

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected ConfigSnapshot() {
    }

    public ConfigSnapshot(String serviceId, String env, Map<String, Object> configJson) {
        this.id = UUID.randomUUID();
        this.serviceId = serviceId;
        this.env = env;
        this.configJson = configJson;
        this.capturedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getEnv() {
        return env;
    }

    public Map<String, Object> getConfigJson() {
        return configJson;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
