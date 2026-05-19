package com.digitalbank.transaction.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A single entry in the bank statement")
public record StatementTransactionRow(

        @Schema(description = "Transaction ID")
        UUID id,

        @Schema(description = "Human-readable transaction type", example = "Transferência")
        String typeLabel,

        @Schema(description = "Movement direction from this account's perspective", example = "DEBIT",
                allowableValues = {"CREDIT", "DEBIT"})
        String direction,

        @Schema(description = "Transaction amount in BRL", example = "1500.00")
        BigDecimal amount,

        @Schema(description = "Counterpart account ID (the other side of the operation)")
        UUID counterpartAccountId,

        @Schema(description = "Optional description")
        String description,

        @Schema(description = "Human-readable status", example = "Concluída")
        String statusLabel,

        @Schema(description = "When the transaction was completed")
        LocalDateTime date
) {}
