package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.TokenBlacklistPort;
import com.autohubstore.authservice.domain.repository.RefreshTokenRepository;

/**
 * Input boundary: revoga o access token (blacklist Redis) e o refresh token.
 */
public class LogoutUseCase {

    private final JwtPort jwtPort;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUseCase(JwtPort jwtPort,
                         TokenBlacklistPort tokenBlacklistPort,
                         RefreshTokenRepository refreshTokenRepository) {
        this.jwtPort = jwtPort;
        this.tokenBlacklistPort = tokenBlacklistPort;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void execute(String accessToken, String refreshToken) {
        // Blacklist do access token pelo JTI com TTL residual
        if (accessToken != null && !accessToken.isBlank()) {
            JwtPort.TokenClaims claims = jwtPort.extractClaims(accessToken);
            long remainingTtl = jwtPort.getRemainingTtlSeconds(accessToken);
            if (remainingTtl > 0) {
                tokenBlacklistPort.blacklist(claims.jti(), remainingTtl);
            }
        }

        // Revogação do refresh token
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        }
    }
}
