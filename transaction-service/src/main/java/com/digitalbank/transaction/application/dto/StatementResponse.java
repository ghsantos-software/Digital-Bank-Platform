package com.digitalbank.transaction.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bank statement for an account over a given period")
public record StatementResponse(

        @Schema(description = "Account ID")
        UUID accountId,

        @Schema(description = "Statement start date", example = "2024-01-01")
        LocalDate from,

        @Schema(description = "Statement end date", example = "2024-01-31")
        LocalDate to,

        @Schema(description = "Total credits (money in) during the period", example = "5000.00")
        BigDecimal totalCredit,

        @Schema(description = "Total debits (money out) during the period", example = "1200.00")
        BigDecimal totalDebit,

        @Schema(description = "Transactions on the current page")
        List<StatementTransactionRow> transactions,

        @Schema(description = "Current page number (0-based)", example = "0")
        int currentPage,

        @Schema(description = "Total pages available", example = "3")
        int totalPages,

        @Schema(description = "Total transactions in the period", example = "47")
        long totalElements
) {}
