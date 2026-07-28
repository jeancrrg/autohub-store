package com.autohubstore.authservice.domain.service;

import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.repository.PasswordResetTokenRepository;
import com.autohubstore.authservice.domain.repository.RefreshTokenRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Serviço de domínio: encapsula regras de negócio dos tokens.
 * Puro Java — sem anotações de framework.
 */
public class TokenDomainService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long refreshTokenTtlSeconds;
    private final long passwordResetTtlMinutes;

    public TokenDomainService(
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            long refreshTokenTtlSeconds,
            long passwordResetTtlMinutes) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.passwordResetTtlMinutes = passwordResetTtlMinutes;
    }

    /**
     * Gera e persiste um novo refresh token para o usuário.
     * Revoga todos os anteriores (rotation).
     */
    public RefreshToken createRefreshToken(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        String tokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        RefreshToken token = RefreshToken.create(userId, tokenValue, expiresAt);
        return refreshTokenRepository.save(token);
    }

    /**
     * Valida e rotaciona um refresh token existente.
     */
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token não encontrado"));

        if (!existing.isValid()) {
            throw new IllegalStateException("Refresh token inválido ou expirado");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        String newTokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        RefreshToken newToken = RefreshToken.create(existing.getUserId(), newTokenValue, expiresAt);
        return refreshTokenRepository.save(newToken);
    }

    /**
     * Gera e persiste um token de reset de senha.
     */
    public PasswordResetToken createPasswordResetToken(UUID userId) {
        String tokenValue = generateSecureToken();
        PasswordResetToken token = PasswordResetToken.create(userId, tokenValue, passwordResetTtlMinutes);
        return passwordResetTokenRepository.save(token);
    }

    /**
     * Valida e marca como usado um token de reset.
     */
    public PasswordResetToken consumePasswordResetToken(String tokenValue) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token de reset não encontrado"));

        if (!token.isValid()) {
            throw new IllegalStateException("Token de reset inválido, expirado ou já utilizado");
        }

        token.markUsed();
        return passwordResetTokenRepository.save(token);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
