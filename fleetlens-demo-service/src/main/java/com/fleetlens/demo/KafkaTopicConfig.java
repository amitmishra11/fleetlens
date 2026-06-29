package com.fleetlens.demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsTopic(DemoProperties properties) {
        return new NewTopic(properties.getTopic(), 1, (short) 1);
    }
}
