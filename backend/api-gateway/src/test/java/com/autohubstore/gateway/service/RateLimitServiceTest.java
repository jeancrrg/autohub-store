package com.autohubstore.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private static final int PUBLIC_LIMIT = 100;
    private static final int AUTH_LIMIT = 200;
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String CLIENT_KEY = "192.168.0.10:/api/v1/catalog/products";

    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private RateLimitService rateLimitService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void devePermitirPrimeiraRequisicaoDaJanelaEDefinirTtl() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(eq(redisKey), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).expire(redisKey, TTL);
    }

    @Test
    void devePermitirRequisicaoDentroDoLimitePublico() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) PUBLIC_LIMIT));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("teste")
    void deveBloquearRequisicaoQueExcedeLimitePublico() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) PUBLIC_LIMIT + 1));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, false))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void devePermitirRequisicaoAutenticadaDentroDoLimiteMaiorMesmoAcimaDoLimitePublico() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        long countAcimaDoLimitePublico = PUBLIC_LIMIT + 1L;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just(countAcimaDoLimitePublico));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, true))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void deveBloquearRequisicaoAutenticadaQueExcedeLimite() {
        String redisKey = "ratelimit:" + CLIENT_KEY;
        when(valueOperations.increment(redisKey)).thenReturn(Mono.just((long) AUTH_LIMIT + 1));

        StepVerifier.create(rateLimitService.isAllowed(CLIENT_KEY, true))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void deveTratarNovaJanelaAposExpiracaoDoTtlComoContadorReiniciado() {
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
