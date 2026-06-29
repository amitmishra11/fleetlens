package com.fleetlens.config.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fleetlens.common.util.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores config_json as plain JSON text so this entity stays portable across
 * databases (H2 locally, Postgres in production) without a vendor-specific JSON column type.
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return "{}";
        }
        return JsonUtils.toJson(attribute);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonUtils.MAPPER.readValue(dbData, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise config_json", e);
        }
    }
}
