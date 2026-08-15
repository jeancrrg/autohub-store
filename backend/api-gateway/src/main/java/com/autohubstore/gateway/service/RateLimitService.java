package com.autohubstore.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final int PUBLIC_LIMIT = 100;
    private static final int AUTH_LIMIT = 200;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isAllowed(String clientKey, boolean authenticated) {
        int limit = authenticated ? AUTH_LIMIT : PUBLIC_LIMIT;
        String redisKey = "ratelimit:" + clientKey;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(redisKey, TTL).thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                });
    }

}
