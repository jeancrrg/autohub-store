package com.autohubstore.authservice.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    private static final int SECONDS_PER_MINUTE = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    public static PasswordResetToken create(UUID userId, String token, long ttlMinutes) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.userId = userId;
        resetToken.token = token;
        resetToken.expiresAt = Instant.now().plusSeconds(ttlMinutes * SECONDS_PER_MINUTE);
        resetToken.used = false;
        return resetToken;
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

}
