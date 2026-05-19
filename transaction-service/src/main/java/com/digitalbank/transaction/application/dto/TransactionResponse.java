package com.digitalbank.transaction.application.dto;

import com.digitalbank.transaction.domain.model.TransactionStatus;
import com.digitalbank.transaction.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Transaction details")
public record TransactionResponse(

        @Schema(description = "Unique transaction identifier")
        UUID id,

        @Schema(description = "Idempotency key used on creation")
        UUID idempotencyKey,

        @Schema(description = "Source account ID")
        UUID sourceAccountId,

        @Schema(description = "Destination account ID (null for DEPOSIT and WITHDRAWAL)")
        UUID destinationAccountId,

        @Schema(description = "Transaction amount in BRL", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Transaction type", example = "DEPOSIT")
        TransactionType type,

        @Schema(description = "Human-readable transaction type", example = "Depósito")
        String typeLabel,

        @Schema(description = "Current processing status", example = "COMPLETED")
        TransactionStatus status,

        @Schema(description = "Human-readable status", example = "Concluída")
        String statusLabel,

        @Schema(description = "Optional description")
        String description,

        @Schema(description = "Reason for failure (null unless status = FAILED)")
        String failureReason,

        @Schema(description = "Keycloak user ID who initiated the transaction")
        String performedBy,

        @Schema(description = "When the transaction was created")
        LocalDateTime createdAt,

        @Schema(description = "When the transaction status was last updated")
        LocalDateTime updatedAt
) {}
