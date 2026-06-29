package com.fleetlens.config;

import com.fleetlens.common.registry.ServiceDefinition;
import com.fleetlens.common.registry.ServiceDirectory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceRegistrySeeder {

    private final ServiceRegistryProperties staticConfig;
    private final ServiceDirectory directory;

    public ServiceRegistrySeeder(ServiceRegistryProperties staticConfig, ServiceDirectory directory) {
        this.staticConfig = staticConfig;
        this.directory = directory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        for (ServiceRegistryProperties.ServiceDefinitionProps svc : staticConfig.getServices()) {
            directory.register(new ServiceDefinition(
                    svc.getId(), svc.getBaseUrl(), svc.getEnv(),
                    svc.getJmxHost(), svc.getJmxPort(), svc.getKafkaConsumerGroups(),
                    false));
        }
    }
}
