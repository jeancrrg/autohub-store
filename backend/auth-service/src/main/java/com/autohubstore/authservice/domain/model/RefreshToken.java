package com.autohubstore.authservice.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domínio: token de refresh persistido no PostgreSQL.
 * Puro Java — sem anotações de framework.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean revoked;

    public RefreshToken(UUID id, UUID userId, String token,
                        Instant expiresAt, Instant createdAt, boolean revoked) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
    }

    public static RefreshToken create(UUID userId, String token, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, token, expiresAt, Instant.now(), false);
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRevoked() { return revoked; }

}
