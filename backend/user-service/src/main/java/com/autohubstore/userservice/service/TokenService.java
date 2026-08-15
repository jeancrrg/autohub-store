package com.autohubstore.userservice.service;

import com.autohubstore.userservice.domain.entity.PasswordResetToken;
import com.autohubstore.userservice.domain.entity.RefreshToken;
import com.autohubstore.userservice.exception.InvalidTokenException;
import com.autohubstore.userservice.repository.PasswordResetTokenRepository;
import com.autohubstore.userservice.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 64;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long refreshTokenTtlSeconds;
    private final long passwordResetTtlMinutes;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenTtlMs,
            @Value("${auth.password-reset-ttl-minutes:15}") long passwordResetTtlMinutes) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenTtlSeconds = refreshTokenTtlMs / MILLIS_PER_SECOND;
        this.passwordResetTtlMinutes = passwordResetTtlMinutes;
    }

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        String tokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        RefreshToken token = RefreshToken.create(userId, tokenValue, expiresAt);
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token não encontrado"));

        if (!existing.isValid()) {
            throw new InvalidTokenException("Refresh token inválido ou expirado");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        String newTokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        RefreshToken newToken = RefreshToken.create(existing.getUserId(), newTokenValue, expiresAt);
        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public PasswordResetToken createPasswordResetToken(UUID userId) {
        String tokenValue = generateSecureToken();
        PasswordResetToken token = PasswordResetToken.create(userId, tokenValue, passwordResetTtlMinutes);
        return passwordResetTokenRepository.save(token);
    }

    @Transactional
    public PasswordResetToken consumePasswordResetToken(String tokenValue) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Token de reset não encontrado"));

        if (!token.isValid()) {
            throw new InvalidTokenException("Token de reset inválido, expirado ou já utilizado");
        }

        token.markUsed();
        return passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
