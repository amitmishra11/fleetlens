package com.fleetlens.config.differ;

public record ConfigChange(String key, Object oldValue, Object newValue, ChangeType changeType) {
}
