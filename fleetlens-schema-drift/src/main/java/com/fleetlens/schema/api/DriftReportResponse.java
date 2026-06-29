package com.fleetlens.schema.api;

import com.fleetlens.schema.detector.DriftFinding;
import com.fleetlens.schema.detector.DriftReport;

import java.util.List;

public record DriftReportResponse(String topic, int fromVersion, int toVersion,
                                   List<FindingResponse> findings, boolean hasBreaking) {

    public record FindingResponse(String field, String findingType, boolean isBreaking) {
    }

    public static DriftReportResponse from(DriftReport report) {
        List<FindingResponse> findings = report.findings().stream()
                .map(DriftReportResponse::toFindingResponse)
                .toList();
        return new DriftReportResponse(report.topic(), report.fromVersion(), report.toVersion(),
                findings, report.hasBreaking());
    }

    private static FindingResponse toFindingResponse(DriftFinding finding) {
        return new FindingResponse(finding.fieldName(), finding.type().name(), finding.breaking());
    }
}
