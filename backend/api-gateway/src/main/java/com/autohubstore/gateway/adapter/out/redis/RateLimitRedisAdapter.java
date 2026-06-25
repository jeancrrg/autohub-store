package com.autohubstore.gateway.adapter.out.redis;

import com.autohubstore.gateway.domain.port.out.RateLimitPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitRedisAdapter implements RateLimitPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitRedisAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Long> increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Mono<Boolean> setExpiry(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

}
