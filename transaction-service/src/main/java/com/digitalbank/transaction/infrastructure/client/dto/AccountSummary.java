package com.digitalbank.transaction.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummary(
        UUID id,
        String accountNumber,
        String branch,
        String type,
        BigDecimal balance,
        String status
) {}
