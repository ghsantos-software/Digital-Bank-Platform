package com.digitalbank.account.infrastructure.messaging;

import com.digitalbank.account.application.service.AccountService;
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
public class TransactionEventConsumer {

    private final AccountService accountService;
    private final BalanceUpdatedPublisher balanceUpdatedPublisher;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
            topics = "transaction.created",
            groupId = "account-transaction-group",
            properties = "spring.json.value.default.type=com.digitalbank.account.infrastructure.messaging.TransactionCreatedEvent"
    )
    public void onTransactionCreated(
            @Payload TransactionCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Processing [{}] offset={} — transactionId: {}, type: {}",
                topic, offset, event.transactionId(), event.type());

        boolean success = false;
        String failureReason = null;

        try {
            accountService.processTransaction(event);
            success = true;
            log.info("Balance updated successfully for transactionId: {}", event.transactionId());
        } catch (Exception ex) {
            failureReason = ex.getMessage();
            log.error("Failed to update balance for transactionId: {} — {}",
                    event.transactionId(), failureReason);
        }

        balanceUpdatedPublisher.publish(new BalanceUpdatedEvent(
                event.transactionId(),
                success,
                failureReason,
                LocalDateTime.now()
        ));
    }
}
