package com.digitalbank.transaction.infrastructure.messaging;

import com.digitalbank.transaction.application.service.TransactionService;
import com.digitalbank.transaction.domain.model.TransactionType;
import com.digitalbank.transaction.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceUpdatedConsumer {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
            topics = "balance.updated",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=com.digitalbank.transaction.infrastructure.messaging.BalanceUpdatedEvent"
    )
    public void onBalanceUpdated(
            @Payload BalanceUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received [{}] offset={} — transactionId: {}, success: {}",
                topic, offset, event.transactionId(), event.success());

        if (event.success()) {
            transactionService.markCompleted(event.transactionId());
            publishTransferCompletedIfApplicable(event);
        } else {
            transactionService.markFailed(event.transactionId(), event.failureReason());
        }
    }

    private void publishTransferCompletedIfApplicable(BalanceUpdatedEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(tx -> {
            if (tx.getType() == TransactionType.TRANSFER) {
                eventPublisher.publishTransferCompleted(new TransferCompletedEvent(
                        tx.getId(),
                        tx.getSourceAccountId(),
                        tx.getDestinationAccountId(),
                        tx.getAmount(),
                        LocalDateTime.now()
                ));
            }
        });
    }
}
