package com.fleetlens.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private String topic = "order-events";
    private long produceIntervalMs = 1000;
    private long consumerDelayMs = 1500;
    private String schemaVariant = "v1";
    private Map<String, Boolean> featureFlags = Map.of();
    private int discountPercent = 5;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public long getProduceIntervalMs() { return produceIntervalMs; }
    public void setProduceIntervalMs(long produceIntervalMs) { this.produceIntervalMs = produceIntervalMs; }

    public long getConsumerDelayMs() { return consumerDelayMs; }
    public void setConsumerDelayMs(long consumerDelayMs) { this.consumerDelayMs = consumerDelayMs; }

    public String getSchemaVariant() { return schemaVariant; }
    public void setSchemaVariant(String schemaVariant) { this.schemaVariant = schemaVariant; }

    public Map<String, Boolean> getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(Map<String, Boolean> featureFlags) { this.featureFlags = featureFlags; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
}
