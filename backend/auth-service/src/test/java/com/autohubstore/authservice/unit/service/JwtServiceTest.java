package com.autohubstore.authservice.unit.service;

import com.autohubstore.authservice.domain.dto.TokenClaims;
import com.autohubstore.authservice.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "cliente@autohubstore.com";
    private static final List<String> ROLES = List.of("CUSTOMER");

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ONE_HOUR_MS);
    }

    @Test
    @DisplayName("Deve gerar access token contendo os claims esperados")
    void shouldGenerateAccessTokenWithExpectedClaims() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

        TokenClaims claims = jwtService.extractClaims(token);

        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.email()).isEqualTo(EMAIL);
        assertThat(claims.roles()).containsExactly("CUSTOMER");
        assertThat(claims.jti()).isNotBlank();
    }

    @Test
    @DisplayName("Deve retornar TTL residual maior que zero quando token ainda for valido")
    void shouldReturnRemainingTtlSecondsGreaterThanZeroForValidToken() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

        long remaining = jwtService.getRemainingTtlSeconds(token);

        assertThat(remaining).isPositive();
    }

    @Test
    @DisplayName("Deve retornar TTL residual zero quando token estiver expirado")
    void shouldReturnZeroRemainingTtlSecondsForExpiredToken() {
        JwtService expiredJwtService = new JwtService(SECRET, -ONE_HOUR_MS);
        String token = expiredJwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

        long remaining = expiredJwtService.getRemainingTtlSeconds(token);

        assertThat(remaining).isZero();
    }

}
