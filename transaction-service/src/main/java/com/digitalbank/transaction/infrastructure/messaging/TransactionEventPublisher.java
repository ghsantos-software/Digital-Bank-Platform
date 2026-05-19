package com.digitalbank.transaction.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    static final String TRANSACTION_CREATED_TOPIC  = "transaction.created";
    static final String TRANSFER_COMPLETED_TOPIC   = "transfer.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        log.info("Publishing TransactionCreatedEvent — transactionId: {}, type: {}",
                event.transactionId(), event.type());
        kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.transactionId().toString(), event);
    }

    public void publishTransferCompleted(TransferCompletedEvent event) {
        log.info("Publishing TransferCompletedEvent — transactionId: {}, amount: R$ {}",
                event.transactionId(), event.amount());
        kafkaTemplate.send(TRANSFER_COMPLETED_TOPIC, event.transactionId().toString(), event);
    }
}
