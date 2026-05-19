package com.digitalbank.account.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic balanceUpdatedTopic() {
        return TopicBuilder.name("balance.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Declaring topic owned by transaction-service so account-service
    // can create it if it starts before transaction-service
    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name("transaction.created")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
