package com.digitalbank.account.infrastructure.messaging;

import com.digitalbank.account.application.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final AccountService accountService;

    @KafkaListener(topics = "user.created", groupId = "${spring.kafka.consumer.group-id}")
    public void onUserCreated(
            @Payload UserCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received [{}] offset={} — userId: {}, email: {}",
                topic, offset, event.userId(), event.email());

        try {
            accountService.createDefault(event.userId());
        } catch (Exception ex) {
            // Log and continue — idempotency is handled at the DB level
            log.error("Failed to create default account for userId: {} — {}",
                    event.userId(), ex.getMessage(), ex);
        }
    }
}
