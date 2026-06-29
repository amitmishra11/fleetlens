package com.fleetlens.config.differ;

import java.util.List;
import java.util.stream.Collectors;

public record ConfigDiff(List<ConfigChange> changes) {

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public List<String> getChangedKeys() {
        return changes.stream().map(ConfigChange::key).collect(Collectors.toList());
    }
}
