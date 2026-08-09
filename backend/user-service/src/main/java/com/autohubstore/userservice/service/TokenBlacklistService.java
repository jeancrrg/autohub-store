package com.autohubstore.userservice.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(final String jti, final long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + jti, "revoked", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(final String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }

}
