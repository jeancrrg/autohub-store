package com.autohubstore.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isBlacklisted(String jti) {
        return redisTemplate.hasKey(KEY_PREFIX + jti);
    }

}
