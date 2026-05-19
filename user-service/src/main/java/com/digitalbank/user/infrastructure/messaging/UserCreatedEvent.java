package com.digitalbank.user.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String email,
        String fullName,
        LocalDateTime occurredAt
) {}
