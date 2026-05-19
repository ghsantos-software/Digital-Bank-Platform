package com.digitalbank.transaction.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Projection of account-service AccountResponse — only fields needed by transaction-service
public record AccountSummary(
        UUID id,
        String accountNumber,
        String branch,
        String type,
        BigDecimal balance,
        String status
) {}
