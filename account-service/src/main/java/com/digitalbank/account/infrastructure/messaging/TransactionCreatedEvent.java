package com.digitalbank.account.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String type,          // "DEPOSIT" | "WITHDRAWAL" | "TRANSFER"
        LocalDateTime occurredAt
) {}
