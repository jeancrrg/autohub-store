package com.autohubstore.authservice.domain.repository;

import com.autohubstore.authservice.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Output boundary — implementada pela camada de infraestrutura (JPA).
 */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void revokeAllByUserId(UUID userId);
}
