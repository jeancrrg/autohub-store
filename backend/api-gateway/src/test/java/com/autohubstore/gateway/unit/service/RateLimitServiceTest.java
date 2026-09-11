package com.autohubstore.gateway.unit.service;

import com.autohubstore.gateway.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    private static final int PUBLIC_LIMIT = 100;
    private static final int AUTH_LIMIT = 200;
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String CLIENT_KEY = "192.168.0.10:/api/v1/catalog/products";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Deve permitir a primeira requisicao da janela e definir o TTL")
    void shouldAllowFirstRequestOfWindowAndSetTtl() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(eq(redisKey), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).expire(redisKey, TTL);
    }

    @Test
    @DisplayName("Deve permitir requisicao dentro do limite publico")
    void shouldAllowRequestWithinPublicLimit() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) PUBLIC_LIMIT));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve bloquear requisicao que excede o limite publico")
    void shouldBlockRequestExceedingPublicLimit() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) PUBLIC_LIMIT + 1));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve permitir requisicao autenticada dentro do limite maior mesmo acima do limite publico")
    void shouldAllowAuthenticatedRequestWithinHigherLimitEvenAbovePublicLimit() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        long countAbovePublicLimit = PUBLIC_LIMIT + 1L;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just(countAbovePublicLimit));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, true))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve bloquear requisicao autenticada que excede o limite")
    void shouldBlockAuthenticatedRequestExceedingLimit() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) AUTH_LIMIT + 1));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, true))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve tratar nova janela apos expiracao do TTL como contador reiniciado")
    void shouldTreatNewWindowAfterTtlExpirationAsResetCounter() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(eq(redisKey), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate, times(2)).expire(redisKey, TTL);
    }

}
