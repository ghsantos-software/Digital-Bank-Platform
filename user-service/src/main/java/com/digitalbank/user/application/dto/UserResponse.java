package com.digitalbank.user.application.dto;

import com.digitalbank.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "User data returned by the API")
public record UserResponse(

        @Schema(description = "Unique user identifier")
        UUID id,

        @Schema(description = "Full name")
        String fullName,

        @Schema(description = "E-mail address")
        String email,

        @Schema(description = "CPF (formatted as stored)")
        String cpf,

        @Schema(description = "Date of birth")
        LocalDate birthDate,

        @Schema(description = "Current account status")
        UserStatus status,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt
) {}
