package com.autohubstore.userservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String email,
        String fullName,
        Instant createdAt
) {

}
