package com.autohubstore.gateway.unit.service;

import com.autohubstore.gateway.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String JTI = "22222222-2222-2222-2222-222222222222";
    private static final String BLACKLIST_KEY = "token:blacklist:" + JTI;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Deve retornar verdadeiro quando o token estiver na blacklist")
    void shouldReturnTrueWhenTokenIsBlacklisted() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(Mono.just(true));

        StepVerifier.create(tokenBlacklistService.isBlacklisted(JTI))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve retornar falso quando o token nao estiver na blacklist")
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(Mono.just(false));

        StepVerifier.create(tokenBlacklistService.isBlacklisted(JTI))
                .expectNext(false)
                .verifyComplete();
    }

}
