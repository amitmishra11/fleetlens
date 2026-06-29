package com.fleetlens.trace.diff;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceDiffEngineTest {

    private final TraceDiffEngine engine = new TraceDiffEngine();

    @Test
    void identicalObjectsProduceNoDiffs() {
        Map<String, Object> a = baseMap();
        Map<String, Object> b = baseMap();

        TraceDiff diff = engine.diff(a, b);

        assertThat(diff.diffCount()).isZero();
        assertThat(diff.fieldDiffs()).isEmpty();
    }

    @Test
    void changedFieldValueIsDetected() {
        Map<String, Object> expected = baseMap();
        Map<String, Object> actual = baseMap();
        actual.put("status", "FAILED");

        TraceDiff diff = engine.diff(expected, actual);

        assertThat(diff.diffCount()).isEqualTo(1);
        FieldDiff fieldDiff = diff.fieldDiffs().get(0);
        assertThat(fieldDiff.path()).isEqualTo("$.status");
        assertThat(fieldDiff.changeType()).isEqualTo(FieldDiff.ChangeType.CHANGED);
        assertThat(fieldDiff.oldValue()).isEqualTo("OK");
        assertThat(fieldDiff.newValue()).isEqualTo("FAILED");
    }

    @Test
    void addedAndRemovedFieldsAreDetected() {
        Map<String, Object> expected = baseMap();
        expected.put("legacyField", "gone-soon");

        Map<String, Object> actual = baseMap();
        actual.put("newField", "fresh");

        TraceDiff diff = engine.diff(expected, actual);

        assertThat(diff.diffCount()).isEqualTo(2);
        assertThat(diff.fieldDiffs())
                .anySatisfy(fd -> {
                    assertThat(fd.path()).isEqualTo("$.legacyField");
                    assertThat(fd.changeType()).isEqualTo(FieldDiff.ChangeType.REMOVED);
                })
                .anySatisfy(fd -> {
                    assertThat(fd.path()).isEqualTo("$.newField");
                    assertThat(fd.changeType()).isEqualTo(FieldDiff.ChangeType.ADDED);
                });
    }

    private Map<String, Object> baseMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", "abc-123");
        map.put("status", "OK");
        map.put("total", 42.5);
        return map;
    }
}
