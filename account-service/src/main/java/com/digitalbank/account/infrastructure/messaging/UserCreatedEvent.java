package com.digitalbank.account.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

// Mirror of user-service event — consumed by account-service to create a default account
public record UserCreatedEvent(
        UUID userId,
        String email,
        String fullName,
        LocalDateTime occurredAt
) {}
