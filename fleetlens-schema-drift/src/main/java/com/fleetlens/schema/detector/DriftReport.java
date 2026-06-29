package com.fleetlens.schema.detector;

import java.util.List;

public record DriftReport(String topic, int fromVersion, int toVersion, List<DriftFinding> findings,
                           boolean hasBreaking) {

    public boolean hasFindings() {
        return !findings.isEmpty();
    }
}
