package com.digitalbank.account.application.dto;

import com.digitalbank.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload to open a new bank account")
public record CreateAccountRequest(

        @Schema(description = "Owner user ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "User ID is required")
        java.util.UUID userId,

        @Schema(description = "Account type", allowableValues = {"CHECKING", "SAVINGS"})
        @NotNull(message = "Account type is required")
        AccountType type,

        @Schema(description = "Branch number (default: 0001)", example = "0001")
        @Pattern(regexp = "\\d{4}", message = "Branch must be a 4-digit number")
        String branch
) {}
