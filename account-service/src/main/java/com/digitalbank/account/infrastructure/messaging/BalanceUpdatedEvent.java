package com.digitalbank.account.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published to "balance.updated" after account-service processes a TransactionCreatedEvent.
 * Consumed by transaction-service to mark the transaction COMPLETED or FAILED.
 */
public record BalanceUpdatedEvent(
        UUID transactionId,
        boolean success,
        String failureReason,
        LocalDateTime occurredAt
) {}
