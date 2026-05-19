package com.digitalbank.transaction.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Projection of account-service AccountResponse — only fields needed by transaction-service
public record AccountBalanceResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String status
) {}
