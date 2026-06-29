package com.fleetlens.schema.detector;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriftAnalyserTest {

    private final DriftAnalyser analyser = new DriftAnalyser();

    @Test
    void fieldRemovalIsBreaking() {
        InferredSchema prev = schema(1, Map.of(
            "id", Set.of(JsonNodeType.STRING),
            "amount", Set.of(JsonNodeType.NUMBER)
        ));
        InferredSchema next = schema(2, Map.of(
            "id", Set.of(JsonNodeType.STRING)
        ));

        DriftReport report = analyser.analyse(prev, next);

        assertTrue(report.hasBreaking());
        DriftFinding finding = report.findings().stream()
            .filter(f -> f.fieldName().equals("amount"))
            .findFirst().orElseThrow();
        assertEquals(FindingType.FIELD_REMOVED, finding.type());
        assertTrue(finding.breaking());
    }

    @Test
    void fieldAdditionIsNonBreaking() {
        InferredSchema prev = schema(1, Map.of(
            "id", Set.of(JsonNodeType.STRING)
        ));
        InferredSchema next = schema(2, Map.of(
            "id", Set.of(JsonNodeType.STRING),
            "newField", Set.of(JsonNodeType.BOOLEAN)
        ));

        DriftReport report = analyser.analyse(prev, next);

        assertFalse(report.hasBreaking());
        DriftFinding finding = report.findings().stream()
            .filter(f -> f.fieldName().equals("newField"))
            .findFirst().orElseThrow();
        assertEquals(FindingType.FIELD_ADDED, finding.type());
        assertFalse(finding.breaking());
    }

    @Test
    void typeChangeIsBreaking() {
        InferredSchema prev = schema(1, Map.of(
            "amount", Set.of(JsonNodeType.NUMBER)
        ));
        InferredSchema next = schema(2, Map.of(
            "amount", Set.of(JsonNodeType.STRING)
        ));

        DriftReport report = analyser.analyse(prev, next);

        assertTrue(report.hasBreaking());
        DriftFinding finding = report.findings().stream()
            .filter(f -> f.fieldName().equals("amount"))
            .findFirst().orElseThrow();
        assertEquals(FindingType.TYPE_CHANGED, finding.type());
        assertTrue(finding.breaking());
    }

    private InferredSchema schema(int version, Map<String, Set<JsonNodeType>> fields) {
        return InferredSchema.fromFieldMap("orders", version, new LinkedHashMap<>(fields));
    }
}
