package com.autohubstore.gateway.unit.service;

import com.autohubstore.gateway.model.JwtClaims;
import com.autohubstore.gateway.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";
    private static final String OTHER_SECRET = "b3V0cm8tc2VncmVkby1kaWZlcmVudGUtcGFyYS10ZXN0ZS0zMmJ5dGVz";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    @DisplayName("Deve validar token valido e retornar claims")
    void shouldValidateValidTokenAndReturnClaims() {
        String userId = "11111111-1111-1111-1111-111111111111";
        String email = "cliente@autohubstore.com";
        String token = buildToken(userId, email, List.of("CUSTOMER"), signingKey, ONE_HOUR_MS);

        JwtClaims claims = jwtService.validate(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo(email);
        assertThat(claims.roles()).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("Deve extrair o jti do token quando presente")
    void shouldExtractJtiWhenPresent() {
        String jti = "22222222-2222-2222-2222-222222222222";
        String token = buildTokenWithJti(jti, signingKey);

        JwtClaims claims = jwtService.validate(token);

        assertThat(claims.jti()).isEqualTo(jti);
    }

    @Test
    @DisplayName("Deve retornar jti nulo quando o token nao possuir esse claim")
    void shouldReturnNullJtiWhenTokenDoesNotHaveIt() {
        String token = buildToken("user-id", "user@autohubstore.com", List.of("CUSTOMER"), signingKey, ONE_HOUR_MS);

        JwtClaims claims = jwtService.validate(token);

        assertThat(claims.jti()).isNull();
    }

    @Test
    @DisplayName("Deve lancar excecao para token expirado")
    void shouldThrowExceptionForExpiredToken() {
        String token = buildToken("user-id", "user@autohubstore.com", List.of("CUSTOMER"), signingKey,
                -ONE_HOUR_MS);

        assertThatThrownBy(() -> jwtService.validate(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao para token com assinatura invalida")
    void shouldThrowExceptionForTokenWithInvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET));
        String token = buildToken("user-id", "user@autohubstore.com", List.of("CUSTOMER"), otherKey, ONE_HOUR_MS);

        assertThatThrownBy(() -> jwtService.validate(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao para token malformado")
    void shouldThrowExceptionForMalformedToken() {
        assertThatThrownBy(() -> jwtService.validate("token-nao-jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao para token ausente")
    void shouldThrowExceptionForMissingToken() {
        assertThatThrownBy(() -> jwtService.validate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String buildToken(String userId, String email, List<String> roles, SecretKey key, long validityMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private String buildTokenWithJti(String jti, SecretKey key) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + JwtServiceTest.ONE_HOUR_MS);
        return Jwts.builder()
                .id(jti)
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("email", "cliente@autohubstore.com")
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

}
