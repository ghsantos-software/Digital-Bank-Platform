package com.digitalbank.transaction.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by transaction-service after a TRANSFER is confirmed COMPLETED.
 * Can be consumed by notification-service, audit-service, etc. in the future.
 */
public record TransferCompletedEvent(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}
