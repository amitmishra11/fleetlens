package com.fleetlens.config;

import com.fleetlens.common.model.ServiceIdentity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "fleetlens")
public class ServiceRegistryProperties {

    private List<ServiceDefinitionProps> services = new ArrayList<>();

    public List<ServiceDefinitionProps> getServices() {
        return services;
    }

    public void setServices(List<ServiceDefinitionProps> services) {
        this.services = services;
    }

    public static class ServiceDefinitionProps {

        private String id;
        private String baseUrl;
        private String env;
        private String jmxHost;
        private Integer jmxPort;
        private List<String> kafkaConsumerGroups = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public String getJmxHost() {
            return jmxHost;
        }

        public void setJmxHost(String jmxHost) {
            this.jmxHost = jmxHost;
        }

        public Integer getJmxPort() {
            return jmxPort;
        }

        public void setJmxPort(Integer jmxPort) {
            this.jmxPort = jmxPort;
        }

        public List<String> getKafkaConsumerGroups() {
            return kafkaConsumerGroups;
        }

        public void setKafkaConsumerGroups(List<String> kafkaConsumerGroups) {
            this.kafkaConsumerGroups = kafkaConsumerGroups;
        }

        public ServiceIdentity toServiceIdentity() {
            return new ServiceIdentity(id, baseUrl, env);
        }
    }
}
