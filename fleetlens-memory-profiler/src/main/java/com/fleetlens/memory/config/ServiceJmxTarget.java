package com.fleetlens.memory.config;

import java.util.List;

public class ServiceJmxTarget {

    private String id;
    private String baseUrl;
    private String env;
    private String jmxHost;
    private int jmxPort;
    private List<String> kafkaConsumerGroups = List.of();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getJmxHost() { return jmxHost; }
    public void setJmxHost(String jmxHost) { this.jmxHost = jmxHost; }

    public int getJmxPort() { return jmxPort; }
    public void setJmxPort(int jmxPort) { this.jmxPort = jmxPort; }

    public List<String> getKafkaConsumerGroups() { return kafkaConsumerGroups; }
    public void setKafkaConsumerGroups(List<String> kafkaConsumerGroups) { this.kafkaConsumerGroups = kafkaConsumerGroups; }
}
