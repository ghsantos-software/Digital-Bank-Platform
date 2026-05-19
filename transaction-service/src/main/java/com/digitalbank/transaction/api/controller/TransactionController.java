package com.digitalbank.transaction.api.controller;

import com.digitalbank.transaction.application.dto.CreateTransactionRequest;
import com.digitalbank.transaction.application.dto.StatementResponse;
import com.digitalbank.transaction.application.dto.TransactionResponse;
import com.digitalbank.transaction.application.service.TransactionService;
import com.digitalbank.transaction.domain.model.TransactionStatus;
import com.digitalbank.transaction.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Transactions", description = "Financial transaction endpoints")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
        summary = "Initiate a financial transaction",
        description = """
            Creates a **DEPOSIT**, **WITHDRAWAL** or **TRANSFER**.

            The transaction is saved as `PENDING` and processed asynchronously via Kafka.
            Account balances are updated by account-service after validation.

            **Idempotency:** supply the same `idempotencyKey` to safely retry without double-processing.
            If the key was already used, the original transaction is returned with HTTP 200.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transaction created (status: PENDING)"),
        @ApiResponse(responseCode = "200", description = "Duplicate request — existing transaction returned"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "422", description = "Business rule violation (insufficient balance, inactive account)")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.create(request);
        HttpStatus status = response.status() == TransactionStatus.PENDING
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Get transaction by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(
            @Parameter(description = "Transaction UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @Operation(
        summary = "List all transactions for an account (paginated)",
        description = "Returns all transactions (any status) where the account is source OR destination, newest first."
    )
    @ApiResponse(responseCode = "200", description = "Page of transactions")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> findByAccountId(
            @Parameter(description = "Account UUID") @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.findByAccountId(accountId, pageable));
    }

    @Operation(
        summary = "Bank statement for an account",
        description = """
            Returns a paginated bank statement showing only **COMPLETED** transactions.

            Each row includes:
            - `direction`: **CREDIT** (money in) or **DEBIT** (money out) from this account's perspective
            - `typeLabel`: human-readable type in Portuguese
            - `counterpartAccountId`: the other account involved in the operation

            The response also includes **period totals** (`totalCredit`, `totalDebit`) calculated
            over the entire filtered period, independent of pagination.

            **Filters (all optional):**
            - `from` / `to` — date range (ISO format: `yyyy-MM-dd`)
            - `type` — one of `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statement returned"),
        @ApiResponse(responseCode = "400", description = "Invalid date or type filter")
    })
    @GetMapping("/account/{accountId}/statement")
    public ResponseEntity<StatementResponse> getStatement(
            @Parameter(description = "Account UUID") @PathVariable UUID accountId,
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filter by transaction type: DEPOSIT, WITHDRAWAL, TRANSFER")
            @RequestParam(required = false) TransactionType type,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.getStatement(accountId, from, to, type, pageable));
    }
}
