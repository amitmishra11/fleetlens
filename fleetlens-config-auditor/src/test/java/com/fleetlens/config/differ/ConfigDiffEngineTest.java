package com.fleetlens.config.differ;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDiffEngineTest {

    private final ConfigDiffEngine engine = new ConfigDiffEngine();

    @Test
    void detectsAddedKey() {
        Map<String, Object> before = Map.of("server.port", "8080");
        Map<String, Object> after = Map.of("server.port", "8080", "feature.flag", "true");

        ConfigDiff diff = engine.diff(before, after);

        assertThat(diff.hasChanges()).isTrue();
        assertThat(diff.getChangedKeys()).containsExactly("feature.flag");

        ConfigChange change = diff.changes().get(0);
        assertThat(change.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(change.oldValue()).isNull();
        assertThat(change.newValue()).isEqualTo("true");
    }

    @Test
    void detectsRemovedKey() {
        Map<String, Object> before = Map.of("server.port", "8080", "feature.flag", "true");
        Map<String, Object> after = Map.of("server.port", "8080");

        ConfigDiff diff = engine.diff(before, after);

        assertThat(diff.hasChanges()).isTrue();
        assertThat(diff.getChangedKeys()).containsExactly("feature.flag");

        ConfigChange change = diff.changes().get(0);
        assertThat(change.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(change.oldValue()).isEqualTo("true");
        assertThat(change.newValue()).isNull();
    }

    @Test
    void detectsModifiedKey() {
        Map<String, Object> before = Map.of("server.port", "8080");
        Map<String, Object> after = Map.of("server.port", "9090");

        ConfigDiff diff = engine.diff(before, after);

        assertThat(diff.hasChanges()).isTrue();
        assertThat(diff.getChangedKeys()).containsExactly("server.port");

        ConfigChange change = diff.changes().get(0);
        assertThat(change.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(change.oldValue()).isEqualTo("8080");
        assertThat(change.newValue()).isEqualTo("9090");
    }

    @Test
    void flattensNestedMapsBeforeComparing() {
        Map<String, Object> before = Map.of(
                "datasource", Map.of("url", "jdbc:postgresql://old-host:5432/db")
        );
        Map<String, Object> after = Map.of(
                "datasource", Map.of("url", "jdbc:postgresql://new-host:5432/db")
        );

        ConfigDiff diff = engine.diff(before, after);

        assertThat(diff.getChangedKeys()).containsExactly("datasource.url");
        assertThat(diff.changes().get(0).changeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    void noChangesWhenMapsAreEqual() {
        Map<String, Object> before = Map.of("server.port", "8080");
        Map<String, Object> after = Map.of("server.port", "8080");

        ConfigDiff diff = engine.diff(before, after);

        assertThat(diff.hasChanges()).isFalse();
        assertThat(diff.getChangedKeys()).isEqualTo(List.of());
    }
}
