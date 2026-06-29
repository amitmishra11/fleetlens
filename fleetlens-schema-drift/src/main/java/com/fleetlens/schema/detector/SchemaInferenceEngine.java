package com.fleetlens.schema.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fleetlens.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Infers a schema by sampling raw JSON messages. Avro + Schema Registry mode is intentionally
 * not implemented in this build: pulling in the Confluent client requires Confluent's Maven
 * repository, which is not configured for this project, so schema drift detection here is
 * JSON-sampling-only (see module README).
 */
@Component
public class SchemaInferenceEngine {

    public InferredSchema inferFromJsonSamples(String topic, List<String> messages) {
        Map<String, Set<JsonNodeType>> fieldTypes = new LinkedHashMap<>();
        for (String msg : messages) {
            JsonNode node;
            try {
                node = JsonUtils.MAPPER.readTree(msg);
            } catch (Exception e) {
                continue; // skip unparseable samples
            }
            if (node == null || !node.isObject()) {
                continue;
            }
            node.fields().forEachRemaining(e ->
                fieldTypes.computeIfAbsent(e.getKey(), k -> new HashSet<>())
                    .add(e.getValue().getNodeType())
            );
        }
        return InferredSchema.fromFieldMap(topic, fieldTypes);
    }
}
