package com.fleetlens.config.api;

import com.fleetlens.common.registry.ServiceDirectory;
import com.fleetlens.config.differ.ConfigDiffEngine;
import com.fleetlens.config.store.ConfigSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ConfigAuditorControllerTest {

    @Mock
    private ConfigSnapshotRepository snapshotRepo;

    private final ServiceDirectory registry = new ServiceDirectory();
    private final ConfigDiffEngine diffEngine = new ConfigDiffEngine();

    private ConfigAuditorController controller() {
        return new ConfigAuditorController(registry, snapshotRepo, diffEngine);
    }

    @Test
    void registeringAServiceMakesItAppearInListServices() {
        ConfigAuditorController controller = controller();

        ResponseEntity<ServiceSummaryResponse> response = controller.registerService(
                new RegisterServiceRequest("local-api", "http://localhost:9000", "local", null, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().managed()).isTrue();
        assertThat(controller.listServices())
                .extracting(ServiceSummaryResponse::id)
                .containsExactly("local-api");
    }

    @Test
    void registeringWithoutAnIdIsRejected() {
        ConfigAuditorController controller = controller();

        assertThatThrownBy(() -> controller.registerService(
                new RegisterServiceRequest(null, "http://localhost:9000", "local", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is required");
    }

    @Test
    void registeringWithoutABaseUrlIsRejected() {
        ConfigAuditorController controller = controller();

        assertThatThrownBy(() -> controller.registerService(
                new RegisterServiceRequest("local-api", "", "local", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl is required");
    }

    @Test
    void aRegisteredServiceCanBeUnregistered() {
        ConfigAuditorController controller = controller();
        controller.registerService(new RegisterServiceRequest("local-api", "http://localhost:9000", null, null, null, null));

        ResponseEntity<Void> response = controller.unregisterService("local-api");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listServices()).isEmpty();
    }

    @Test
    void unregisteringAnUnknownServiceThrowsIllegalArgument() {
        ConfigAuditorController controller = controller();

        assertThatThrownBy(() -> controller.unregisterService("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void staticallySeededServicesCannotBeUnregisteredThroughTheApi() {
        registry.register(new com.fleetlens.common.registry.ServiceDefinition(
                "order-service", "http://localhost:8090", "dev", null, null, List.of(), false));
        ConfigAuditorController controller = controller();

        assertThatThrownBy(() -> controller.unregisterService("order-service"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(controller.listServices()).hasSize(1);
    }
}
