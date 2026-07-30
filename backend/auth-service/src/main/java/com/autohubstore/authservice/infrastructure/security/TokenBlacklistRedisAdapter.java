package com.autohubstore.authservice.infrastructure.security;

import com.autohubstore.authservice.application.port.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenBlacklistRedisAdapter implements TokenBlacklistPort {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + jti, "revoked", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return redisTemplate.hasKey(KEY_PREFIX + jti);
    }

}
