package com.digitalbank.account.application.dto;

import com.digitalbank.account.domain.model.AccountStatus;
import com.digitalbank.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Current account balance snapshot")
public record BalanceResponse(

        @Schema(description = "Account ID")
        UUID accountId,

        @Schema(description = "Account number", example = "10000001")
        String accountNumber,

        @Schema(description = "Branch", example = "0001")
        String branch,

        @Schema(description = "Account type")
        AccountType type,

        @Schema(description = "Current available balance in BRL", example = "1500.00")
        BigDecimal balance,

        @Schema(description = "Account status")
        AccountStatus status,

        @Schema(description = "Timestamp of this balance read")
        LocalDateTime checkedAt
) {}
