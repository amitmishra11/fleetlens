package com.fleetlens.trace.diff;

import java.util.List;

public record TraceDiff(List<FieldDiff> fieldDiffs, int diffCount) {

    public static TraceDiff of(List<FieldDiff> fieldDiffs) {
        return new TraceDiff(fieldDiffs, fieldDiffs.size());
    }

    public boolean hasChanges() {
        return diffCount > 0;
    }
}
