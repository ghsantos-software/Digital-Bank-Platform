package com.digitalbank.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(description = "Payload to register a new user")
public record CreateUserRequest(

        @Schema(description = "Full name", example = "João da Silva")
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Schema(description = "E-mail address (must be unique)", example = "joao@email.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Schema(description = "CPF — formatted (000.000.000-00) or digits only", example = "529.982.247-25")
        @NotBlank(message = "CPF is required")
        @CPF(message = "Invalid CPF")
        String cpf,

        @Schema(description = "Date of birth (ISO 8601)", example = "1990-05-20")
        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {}
