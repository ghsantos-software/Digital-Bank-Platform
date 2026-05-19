package com.digitalbank.account.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceUpdatedPublisher {

    static final String BALANCE_UPDATED_TOPIC = "balance.updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(BalanceUpdatedEvent event) {
        log.info("Publishing BalanceUpdatedEvent — transactionId: {}, success: {}",
                event.transactionId(), event.success());
        kafkaTemplate.send(BALANCE_UPDATED_TOPIC, event.transactionId().toString(), event);
    }
}
