package com.digitalbank.transaction.infrastructure.messaging;

import com.digitalbank.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published to "transaction.created" after a transaction is saved as PENDING.
 * Consumed by account-service to apply the debit/credit on the accounts.
 */
public record TransactionCreatedEvent(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        TransactionType type,
        LocalDateTime occurredAt
) {}
