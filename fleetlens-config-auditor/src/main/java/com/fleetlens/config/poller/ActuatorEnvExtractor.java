package com.fleetlens.config.poller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot's /actuator/env response nests every property inside a list of
 * propertySources, each holding its own {@code properties} map (and each value
 * wrapped as {"value": ...}). Treating that raw shape as "the config" makes every
 * diff/matrix view show one opaque "propertySources" blob instead of real per-key
 * drift. This flattens it into a single effective key -> value map, honouring
 * Spring's source precedence (sources earlier in the list win).
 */
public final class ActuatorEnvExtractor {

    private ActuatorEnvExtractor() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractProperties(Map<String, Object> rawEnv) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (rawEnv == null) {
            return result;
        }

        Object sourcesObj = rawEnv.get("propertySources");
        if (!(sourcesObj instanceof List<?> sources)) {
            return rawEnv;
        }

        for (int i = sources.size() - 1; i >= 0; i--) {
            if (!(sources.get(i) instanceof Map<?, ?> source)) {
                continue;
            }
            Object propsObj = source.get("properties");
            if (!(propsObj instanceof Map<?, ?> props)) {
                continue;
            }
            for (Map.Entry<?, ?> entry : props.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> valueWrapper) {
                    value = valueWrapper.get("value");
                }
                result.put(key, value);
            }
        }
        return result;
    }
}
