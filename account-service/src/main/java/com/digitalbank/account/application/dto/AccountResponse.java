package com.digitalbank.account.application.dto;

import com.digitalbank.account.domain.model.AccountStatus;
import com.digitalbank.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Account data returned by the API")
public record AccountResponse(

        @Schema(description = "Unique account identifier")
        UUID id,

        @Schema(description = "Owner user ID")
        UUID userId,

        @Schema(description = "Auto-generated account number", example = "10000001")
        String accountNumber,

        @Schema(description = "Branch number", example = "0001")
        String branch,

        @Schema(description = "Account type")
        AccountType type,

        @Schema(description = "Current balance in BRL")
        BigDecimal balance,

        @Schema(description = "Current account status")
        AccountStatus status,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt
) {}
