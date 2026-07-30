package com.autohubstore.authservice.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domínio: token temporário para reset de senha (TTL 15 min).
 * Puro Java — sem anotações de framework.
 */
public class PasswordResetToken {

    private static final int SECONDS_PER_MINUTE = 60;

    private final UUID id;
    private final UUID userId;
    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean used;

    public PasswordResetToken(UUID id, UUID userId, String token,
                              Instant expiresAt, Instant createdAt, boolean used) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.used = used;
    }

    public static PasswordResetToken create(UUID userId, String token, long ttlMinutes) {
        return new PasswordResetToken(
                UUID.randomUUID(),
                userId,
                token,
                Instant.now().plusSeconds(ttlMinutes * SECONDS_PER_MINUTE),
                Instant.now(),
                false
        );
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsed() { return used; }

}
