package com.fleetlens.correlation.incident;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores a List&lt;UUID&gt; as a comma-joined VARCHAR column. Simpler and more portable
 * across Hibernate dialects than mapping to a native Postgres uuid[] array type.
 */
@Converter
public class UuidListConverter implements AttributeConverter<List<UUID>, String> {

    @Override
    public String convertToDatabaseColumn(List<UUID> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(","))
            .map(UUID::fromString)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
