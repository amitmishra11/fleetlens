package com.fleetlens.schema.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fleetlens.common.util.JsonUtils;
import com.fleetlens.schema.detector.DriftReport;
import com.fleetlens.schema.detector.InferredSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "schema_versions")
public class SchemaVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_from_prev")
    private String diffFromPrev;

    @Column(name = "is_breaking", nullable = false)
    private boolean breaking;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected SchemaVersion() {
        // JPA
    }

    private SchemaVersion(String topic, int version, String schemaJson, String diffFromPrev, boolean breaking) {
        this.topic = topic;
        this.version = version;
        this.schemaJson = schemaJson;
        this.diffFromPrev = diffFromPrev;
        this.breaking = breaking;
        this.detectedAt = Instant.now();
    }

    public static SchemaVersion initial(String topic, InferredSchema schema) {
        return new SchemaVersion(topic, 1, JsonUtils.toJson(schema.fieldTypes()), null, false);
    }

    public static SchemaVersion next(String topic, InferredSchema schema, DriftReport report) {
        return new SchemaVersion(
            topic,
            report.toVersion(),
            JsonUtils.toJson(schema.fieldTypes()),
            JsonUtils.toJson(report),
            report.hasBreaking()
        );
    }

    public InferredSchema toInferred() {
        Map<String, Set<JsonNodeType>> fieldTypes;
        try {
            fieldTypes = JsonUtils.MAPPER.readValue(schemaJson, new TypeReference<Map<String, Set<JsonNodeType>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise stored schema_json for topic " + topic, e);
        }
        return InferredSchema.fromFieldMap(topic, version, fieldTypes);
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public int getVersion() {
        return version;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public String getDiffFromPrev() {
        return diffFromPrev;
    }

    public boolean isBreaking() {
        return breaking;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
