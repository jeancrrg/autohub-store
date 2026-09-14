package com.autohubstore.authservice.unit.service;

import com.autohubstore.authservice.service.TokenBlacklistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String JTI = "11111111-1111-1111-1111-111111111111";
    private static final long TTL_SECONDS = 3_600L;
    private static final String EXPECTED_KEY = "token:blacklist:" + JTI;
    private static final String EXPECTED_VALUE = "revoked";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Deve registrar o jti na blacklist com o TTL residual do token")
    void shouldRegisterJtiInBlacklistWithRemainingTtl() {
        tokenBlacklistService.blacklist(JTI, TTL_SECONDS);

        verify(valueOperations).set(EXPECTED_KEY, EXPECTED_VALUE, Duration.ofSeconds(TTL_SECONDS));
    }

}
