package com.autohubstore.gateway.adapter.out.redis;

import com.autohubstore.gateway.domain.port.out.RateLimitPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitRedisAdapter implements RateLimitPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Long> increment(final String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Mono<Boolean> setExpiry(final String key, final Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

}
