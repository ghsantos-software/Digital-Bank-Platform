package com.digitalbank.account.api.controller;

import com.digitalbank.account.application.dto.AccountResponse;
import com.digitalbank.account.application.dto.BalanceResponse;
import com.digitalbank.account.application.dto.CreateAccountRequest;
import com.digitalbank.account.application.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts", description = "Bank account management endpoints")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Open a new bank account",
               description = "Creates an account with zero balance and ACTIVE status. Account number is auto-generated.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
    })
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @Operation(summary = "Get account by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(
            @Parameter(description = "Account UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @Operation(
        summary = "Get current account balance",
        description = """
            Returns a real-time balance snapshot for the account.
            Includes `checkedAt` timestamp to indicate when the read occurred.
            Use this endpoint for displaying the current balance on dashboards.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance retrieved"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @Parameter(description = "Account UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @Operation(summary = "List all accounts for a user")
    @ApiResponse(responseCode = "200", description = "List of accounts (may be empty)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> findByUserId(
            @Parameter(description = "Owner user UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.findByUserId(userId));
    }

    @Operation(summary = "Block an account",
               description = "Sets account status to BLOCKED. Transactions will be rejected for blocked accounts.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account blocked"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "422", description = "Account is already blocked or closed")
    })
    @PatchMapping("/{id}/block")
    public ResponseEntity<Void> block(
            @Parameter(description = "Account UUID") @PathVariable UUID id) {
        accountService.block(id);
        return ResponseEntity.noContent().build();
    }
}
