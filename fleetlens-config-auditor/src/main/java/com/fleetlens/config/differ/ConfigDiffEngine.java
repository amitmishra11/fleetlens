package com.fleetlens.config.differ;

import com.fleetlens.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConfigDiffEngine {

    public ConfigDiff diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> flatBefore = JsonUtils.flatten(before);
        Map<String, Object> flatAfter = JsonUtils.flatten(after);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(flatBefore.keySet());
        allKeys.addAll(flatAfter.keySet());

        List<ConfigChange> changes = allKeys.stream()
                .filter(key -> hasChanged(flatBefore, flatAfter, key))
                .map(key -> new ConfigChange(
                        key,
                        flatBefore.get(key),
                        flatAfter.get(key),
                        classifyChange(flatBefore, flatAfter, key)
                ))
                .collect(Collectors.toList());

        return new ConfigDiff(changes);
    }

    private boolean hasChanged(Map<String, Object> before, Map<String, Object> after, String key) {
        boolean inBefore = before.containsKey(key);
        boolean inAfter = after.containsKey(key);
        if (inBefore != inAfter) {
            return true;
        }
        return !Objects.equals(before.get(key), after.get(key));
    }

    private ChangeType classifyChange(Map<String, Object> before, Map<String, Object> after, String key) {
        boolean inBefore = before.containsKey(key);
        boolean inAfter = after.containsKey(key);
        if (!inBefore) {
            return ChangeType.ADDED;
        }
        if (!inAfter) {
            return ChangeType.REMOVED;
        }
        return ChangeType.MODIFIED;
    }
}
