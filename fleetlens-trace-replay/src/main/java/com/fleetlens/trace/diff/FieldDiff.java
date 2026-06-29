package com.fleetlens.trace.diff;

public record FieldDiff(String path, ChangeType changeType, Object oldValue, Object newValue) {

    public enum ChangeType {
        ADDED,
        REMOVED,
        CHANGED
    }
}
