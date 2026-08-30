package com.autohubstore.authservice.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + jti, "revoked", Duration.ofSeconds(ttlSeconds));
    }

}
