package com.autohubstore.authservice.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Port para operações JWT — implementado pela infraestrutura (JwtService).
 */
public interface JwtPort {

    String generateAccessToken(UUID userId, String email, List<String> roles);

    TokenClaims extractClaims(String token);

    boolean isTokenExpired(String token);

    /**
     * Retorna os segundos restantes até a expiração do token.
     */
    long getRemainingTtlSeconds(String token);

    record TokenClaims(String jti, UUID userId, String email, List<String> roles) {}
}
