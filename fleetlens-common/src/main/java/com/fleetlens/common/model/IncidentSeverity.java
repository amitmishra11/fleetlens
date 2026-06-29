package com.fleetlens.common.model;

public enum IncidentSeverity {
    INFO,
    WARN,
    CRITICAL;

    public boolean isHigherThan(IncidentSeverity other) {
        return this.ordinal() > other.ordinal();
    }
}
