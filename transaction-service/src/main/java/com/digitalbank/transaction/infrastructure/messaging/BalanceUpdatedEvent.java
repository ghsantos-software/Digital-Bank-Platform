package com.digitalbank.transaction.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by account-service after processing a TransactionCreatedEvent.
 * Consumed by transaction-service to mark the transaction COMPLETED or FAILED.
 */
public record BalanceUpdatedEvent(
        UUID transactionId,
        boolean success,
        String failureReason,
        LocalDateTime occurredAt
) {}
