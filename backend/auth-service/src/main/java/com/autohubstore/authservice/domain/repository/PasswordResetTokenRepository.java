package com.autohubstore.authservice.domain.repository;

import com.autohubstore.authservice.domain.model.PasswordResetToken;

import java.util.Optional;

/**
 * Output boundary — implementada pela camada de infraestrutura (JPA).
 */
public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);
}
