package com.fleetlens.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fleetlens")
public class MemoryProfilerProperties {

    private List<ServiceJmxTarget> services = List.of();

    public List<ServiceJmxTarget> getServices() { return services; }
    public void setServices(List<ServiceJmxTarget> services) { this.services = services; }
}
