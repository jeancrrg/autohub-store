package com.autohubstore.gateway.domain.port.out;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface RateLimitPort {

    Mono<Long> increment(String key);

    Mono<Boolean> setExpiry(String key, Duration ttl);

}
