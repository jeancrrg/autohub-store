package com.autohubstore.userservice.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado no Kafka após cadastro bem-sucedido.
 * Tópico: user.created
 */
public record UserCreatedEvent(
        UUID userId,
        String email,
        String fullName,
        Instant createdAt
) {}
