package com.fleetlens.schema.api;

public record TopicSummary(String topic, int latestVersion, boolean hasBreakingHistory) {
}
