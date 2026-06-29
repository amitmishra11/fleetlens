package com.fleetlens.schema.detector;

public record DriftFinding(String fieldName, FindingType type, boolean breaking) {
}
