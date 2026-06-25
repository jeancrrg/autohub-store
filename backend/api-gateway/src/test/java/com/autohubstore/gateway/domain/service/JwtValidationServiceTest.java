package com.autohubstore.gateway.domain.service;

import com.autohubstore.gateway.domain.model.JwtClaims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtValidationServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";

    private JwtValidationService service;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        service = new JwtValidationService(SECRET);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    void validate_validToken_returnsClaims() {
        String token = Jwts.builder()
                .subject("user-42")
                .claim("email", "user@autohub.com")
                .claim("roles", List.of("USER"))
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();

        JwtClaims claims = service.validate(token);

        assertThat(claims.userId()).isEqualTo("user-42");
        assertThat(claims.email()).isEqualTo("user@autohub.com");
        assertThat(claims.roles()).containsExactly("USER");
    }

    @Test
    void validate_multipleRoles_returnsAllRoles() {
        String token = Jwts.builder()
                .subject("admin-1")
                .claim("email", "admin@autohub.com")
                .claim("roles", List.of("ADMIN", "USER"))
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();

        JwtClaims claims = service.validate(token);

        assertThat(claims.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void validate_noRolesClaim_returnsEmptyRoles() {
        String token = Jwts.builder()
                .subject("user-99")
                .claim("email", "noroles@autohub.com")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();

        JwtClaims claims = service.validate(token);

        assertThat(claims.roles()).isEmpty();
    }

    @Test
    void validate_expiredToken_throwsException() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("email", "expired@autohub.com")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> service.validate(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validate_wrongSignature_throwsException() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("d3JvbmdLZXktd3JvbmdLZXktd3JvbmdLZXktd3JvbmdLZXk="));
        String token = Jwts.builder()
                .subject("user-1")
                .claim("email", "tampered@autohub.com")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(wrongKey)
                .compact();

        assertThatThrownBy(() -> service.validate(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validate_malformedToken_throwsException() {
        assertThatThrownBy(() -> service.validate("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("email", "valid@autohub.com")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void isValid_invalidToken_returnsFalse() {
        assertThat(service.isValid("garbage.token.here")).isFalse();
    }
}
