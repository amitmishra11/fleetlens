package com.fleetlens.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonUtils() {}

    public static JsonNode toTree(Object value) {
        return MAPPER.valueToTree(value);
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise to JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> flatten(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenInto(source, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenInto(Map<String, Object> source, String prefix, Map<String, Object> sink) {
        source.forEach((key, value) -> {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                flattenInto((Map<String, Object>) nested, fullKey, sink);
            } else {
                sink.put(fullKey, value);
            }
        });
    }
}
