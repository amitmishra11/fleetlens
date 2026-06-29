package com.fleetlens.common.event;

import com.fleetlens.common.model.IncidentSeverity;
import com.fleetlens.common.model.ModuleType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ConfigDriftEvent extends ModuleEvent {

    private final List<String> changedKeys;

    public ConfigDriftEvent(String serviceId, List<String> changedKeys) {
        super(serviceId, ModuleType.CONFIG, IncidentSeverity.WARN, Instant.now());
        this.changedKeys = changedKeys;
    }

    public List<String> getChangedKeys() { return changedKeys; }

    @Override
    public String getSummary() {
        return "Config drift: " + changedKeys.size() + " key(s) changed";
    }

    @Override
    public Map<String, Object> getPayload() {
        return Map.of("changedKeys", changedKeys);
    }
}
