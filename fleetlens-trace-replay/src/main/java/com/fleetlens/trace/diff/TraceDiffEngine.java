package com.fleetlens.trace.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleetlens.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Structural diff between two arbitrary JSON-serialisable objects. Walks both
 * trees field-by-field and records additions, removals and value changes.
 */
@Component
public class TraceDiffEngine {

    public TraceDiff diff(Object expected, Object actual) {
        JsonNode expectedNode = toNode(expected);
        JsonNode actualNode = toNode(actual);

        List<FieldDiff> diffs = new ArrayList<>();
        walk("$", expectedNode, actualNode, diffs);
        return TraceDiff.of(diffs);
    }

    private JsonNode toNode(Object value) {
        if (value instanceof JsonNode node) {
            return node;
        }
        if (value instanceof String s) {
            try {
                return JsonUtils.MAPPER.readTree(s);
            } catch (Exception e) {
                return JsonUtils.MAPPER.getNodeFactory().textNode(s);
            }
        }
        return JsonUtils.toTree(value);
    }

    private void walk(String path, JsonNode expected, JsonNode actual, List<FieldDiff> diffs) {
        boolean expectedMissing = expected == null || expected.isMissingNode() || expected.isNull();
        boolean actualMissing = actual == null || actual.isMissingNode() || actual.isNull();

        if (expectedMissing && actualMissing) {
            return;
        }
        if (expectedMissing) {
            diffs.add(new FieldDiff(path, FieldDiff.ChangeType.ADDED, null, valueOf(actual)));
            return;
        }
        if (actualMissing) {
            diffs.add(new FieldDiff(path, FieldDiff.ChangeType.REMOVED, valueOf(expected), null));
            return;
        }

        if (expected.isObject() && actual.isObject()) {
            walkObject(path, expected, actual, diffs);
        } else if (expected.isArray() && actual.isArray()) {
            walkArray(path, expected, actual, diffs);
        } else if (!expected.equals(actual)) {
            diffs.add(new FieldDiff(path, FieldDiff.ChangeType.CHANGED, valueOf(expected), valueOf(actual)));
        }
    }

    private void walkObject(String path, JsonNode expected, JsonNode actual, List<FieldDiff> diffs) {
        Iterator<String> fieldNames = expected.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            walk(path + "." + field, expected.get(field), actual.get(field), diffs);
        }
        Iterator<String> actualFieldNames = actual.fieldNames();
        while (actualFieldNames.hasNext()) {
            String field = actualFieldNames.next();
            if (!expected.has(field)) {
                walk(path + "." + field, null, actual.get(field), diffs);
            }
        }
    }

    private void walkArray(String path, JsonNode expected, JsonNode actual, List<FieldDiff> diffs) {
        int max = Math.max(expected.size(), actual.size());
        for (int i = 0; i < max; i++) {
            JsonNode e = i < expected.size() ? expected.get(i) : null;
            JsonNode a = i < actual.size() ? actual.get(i) : null;
            walk(path + "[" + i + "]", e, a, diffs);
        }
    }

    private Object valueOf(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject() || node.isArray()) {
            return JsonUtils.MAPPER.convertValue(node, Map.class);
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }
}
