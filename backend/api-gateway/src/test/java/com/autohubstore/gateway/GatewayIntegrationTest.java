package com.autohubstore.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class GatewayIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

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
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.builder()
                .subject("user-1")
                .claim("email", "user-1" + "@autohub.com")
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

}
