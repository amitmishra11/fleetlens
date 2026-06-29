package com.fleetlens.config.poller;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorEnvExtractorTest {

    @Test
    void flattensWrappedValuesFromASinglePropertySource() {
        Map<String, Object> rawEnv = Map.of(
                "propertySources", List.of(
                        Map.of("name", "systemEnvironment", "properties", Map.of(
                                "API_TIMEOUT_MS", Map.of("value", "900000")
                        ))
                )
        );

        Map<String, Object> result = ActuatorEnvExtractor.extractProperties(rawEnv);

        assertThat(result).containsEntry("API_TIMEOUT_MS", "900000");
    }

    @Test
    void earlierSourcesInTheListWinOverLaterOnesForTheSameKey() {
        Map<String, Object> rawEnv = Map.of(
                "propertySources", List.of(
                        Map.of("name", "applicationConfig", "properties", Map.of(
                                "server.port", Map.of("value", "8088")
                        )),
                        Map.of("name", "systemEnvironment", "properties", Map.of(
                                "server.port", Map.of("value", "8080")
                        ))
                )
        );

        Map<String, Object> result = ActuatorEnvExtractor.extractProperties(rawEnv);

        assertThat(result).containsEntry("server.port", "8088");
    }

    @Test
    void returnsEmptyMapForNullInput() {
        assertThat(ActuatorEnvExtractor.extractProperties(null)).isEmpty();
    }

    @Test
    void returnsInputUnchangedWhenThereIsNoPropertySourcesList() {
        Map<String, Object> rawEnv = Map.of("activeProfiles", List.of("dev"));

        assertThat(ActuatorEnvExtractor.extractProperties(rawEnv)).isEqualTo(rawEnv);
    }

    @Test
    void skipsSourcesThatHaveNoPropertiesMap() {
        Map<String, Object> rawEnv = Map.of(
                "propertySources", List.of(
                        Map.of("name", "empty-source"),
                        Map.of("name", "real-source", "properties", Map.of(
                                "FOO", Map.of("value", "bar")
                        ))
                )
        );

        Map<String, Object> result = ActuatorEnvExtractor.extractProperties(rawEnv);

        assertThat(result).containsEntry("FOO", "bar");
    }
}
