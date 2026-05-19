package com.digitalbank.transaction.application.dto;

import com.digitalbank.transaction.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Payload to initiate a financial transaction")
public record CreateTransactionRequest(

        @Schema(description = "Optional client-supplied key to prevent duplicate submissions. " +
                              "Re-sending the same key returns the original transaction without double-processing.",
                example = "a1b2c3d4-1234-5678-abcd-ef1234567890")
        UUID idempotencyKey,

        @Schema(description = "Account to debit (WITHDRAWAL/TRANSFER) or credit (DEPOSIT)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @Schema(description = "Destination account — required only for TRANSFER",
                example = "7fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID destinationAccountId,

        @Schema(description = "Transaction amount in BRL", example = "250.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Transaction type", allowableValues = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"})
        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @Schema(description = "Optional description or memo", example = "Rent payment")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description
) {}
