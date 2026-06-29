package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.Map;

public class SchemaDriftEvent extends ModuleEvent {

    private final String topic;
    private final int findingCount;
    private final boolean breaking;

    public SchemaDriftEvent(String serviceId, String topic, int findingCount, boolean breaking) {
        super(serviceId, ModuleType.SCHEMA_DRIFT,
            breaking ? IncidentSeverity.CRITICAL : IncidentSeverity.INFO, Instant.now());
        this.topic = topic;
        this.findingCount = findingCount;
        this.breaking = breaking;
    }

    public String getTopic() { return topic; }
    public boolean isBreaking() { return breaking; }

    @Override
    public String getSummary() {
        return "Schema drift on topic " + topic + " (" + findingCount + " finding(s))"
            + (breaking ? " [BREAKING]" : "");
    }

    @Override
    public Map<String, Object> getPayload() {
        return Map.of("topic", topic, "findingCount", findingCount, "breaking", breaking);
    }
}
