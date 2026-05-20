package com.digitalbank.transaction.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String status
) {}
