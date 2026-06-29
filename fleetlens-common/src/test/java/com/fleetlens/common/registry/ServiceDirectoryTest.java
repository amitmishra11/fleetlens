package com.fleetlens.common.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDirectoryTest {

    private final ServiceDirectory directory = new ServiceDirectory();

    @Test
    void registersAndListsAService() {
        directory.register(new ServiceDefinition("svc-a", "http://localhost:8080", "dev", null, null, null, true));

        assertThat(directory.list()).hasSize(1);
        assertThat(directory.find("svc-a")).isPresent();
    }

    @Test
    void reRegisteringTheSameIdOverwritesTheExistingDefinition() {
        directory.register(new ServiceDefinition("svc-a", "http://localhost:8080", "dev", null, null, null, true));
        directory.register(new ServiceDefinition("svc-a", "http://localhost:9090", "prod", null, null, null, true));

        assertThat(directory.list()).hasSize(1);
        assertThat(directory.find("svc-a").orElseThrow().baseUrl()).isEqualTo("http://localhost:9090");
    }

    @Test
    void managedServicesCanBeUnregistered() {
        directory.register(new ServiceDefinition("svc-a", "http://localhost:8080", "dev", null, null, null, true));

        assertThat(directory.unregister("svc-a")).isTrue();
        assertThat(directory.list()).isEmpty();
    }

    @Test
    void staticallySeededServicesCannotBeUnregistered() {
        directory.register(new ServiceDefinition("svc-a", "http://localhost:8080", "dev", null, null, null, false));

        assertThat(directory.unregister("svc-a")).isFalse();
        assertThat(directory.list()).hasSize(1);
    }

    @Test
    void unregisteringAnUnknownIdReturnsFalse() {
        assertThat(directory.unregister("does-not-exist")).isFalse();
    }

    @Test
    void normalisesNullKafkaConsumerGroupsAndBlankEnv() {
        directory.register(new ServiceDefinition("svc-a", "http://localhost:8080", "", null, null, null, true));

        ServiceDefinition stored = directory.find("svc-a").orElseThrow();
        assertThat(stored.env()).isEqualTo("dev");
        assertThat(stored.kafkaConsumerGroups()).isEqualTo(List.of());
    }
}
