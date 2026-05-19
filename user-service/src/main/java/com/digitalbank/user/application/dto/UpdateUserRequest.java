package com.digitalbank.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Payload to update user data — all fields are optional")
public record UpdateUserRequest(

        @Schema(description = "New full name", example = "João Silva Sauro")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Schema(description = "New e-mail address", example = "joao.novo@email.com")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Schema(description = "New date of birth", example = "1990-06-15")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {}
