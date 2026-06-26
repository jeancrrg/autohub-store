package com.autohubstore.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void actuatorHealth_noAuth_returns200() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedEndpoint_noToken_returns401() {
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() {
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header("Authorization", "Bearer not.a.real.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_validToken_attemptsRouting() {
        webTestClient.get()
                .uri("/api/v1/orders/1")
                .header("Authorization", "Bearer " + buildValidToken(List.of("USER")))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void publicCatalogEndpoint_noToken_returns5xx() {
        webTestClient.get()
                .uri("/api/v1/catalog/products")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void fallbackEndpoint_noAuth_returns503() {
        webTestClient.get()
                .uri("/fallback/auth")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    private String buildValidToken(List<String> roles) {
        final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.builder()
                .subject("user-1")
                .claim("email", "user-1@autohub.com")
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }
}
