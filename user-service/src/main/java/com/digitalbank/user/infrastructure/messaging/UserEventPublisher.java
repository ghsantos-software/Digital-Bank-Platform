package com.digitalbank.user.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    static final String USER_CREATED_TOPIC = "user.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(UserCreatedEvent event) {
        log.info("Publishing UserCreatedEvent for userId: {}", event.userId());
        kafkaTemplate.send(USER_CREATED_TOPIC, event.userId().toString(), event);
    }
}
