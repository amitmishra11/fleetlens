package com.fleetlens.schema.detector;

import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A schema inferred by sampling messages on a topic: for each top-level field, the set of
 * JSON node types observed across the sample (a field may legitimately appear as more than
 * one type across messages, e.g. a nullable numeric field observed as NUMBER and NULL).
 */
public class InferredSchema {

    private final String topic;
    private final int version;
    private final Map<String, Set<JsonNodeType>> fieldTypes;

    private InferredSchema(String topic, int version, Map<String, Set<JsonNodeType>> fieldTypes) {
        this.topic = topic;
        this.version = version;
        this.fieldTypes = fieldTypes;
    }

    public static InferredSchema fromFieldMap(String topic, Map<String, Set<JsonNodeType>> fieldTypes) {
        return new InferredSchema(topic, 1, new LinkedHashMap<>(fieldTypes));
    }

    public static InferredSchema fromFieldMap(String topic, int version, Map<String, Set<JsonNodeType>> fieldTypes) {
        return new InferredSchema(topic, version, new LinkedHashMap<>(fieldTypes));
    }

    public String topic() {
        return topic;
    }

    public int version() {
        return version;
    }

    public Set<String> fieldNames() {
        return fieldTypes.keySet();
    }

    public Set<JsonNodeType> typeOf(String field) {
        return fieldTypes.getOrDefault(field, Set.of());
    }

    public Map<String, Set<JsonNodeType>> fieldTypes() {
        return fieldTypes;
    }

    public InferredSchema withVersion(int newVersion) {
        return new InferredSchema(topic, newVersion, fieldTypes);
    }
}
