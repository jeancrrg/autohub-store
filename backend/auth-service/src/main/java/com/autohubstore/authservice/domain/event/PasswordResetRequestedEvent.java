package com.autohubstore.authservice.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event publicado no Kafka quando o usuário solicita reset de senha.
 * Tópico: auth.password-reset
 */
public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String resetToken,
        Instant expiresAt
) {}
