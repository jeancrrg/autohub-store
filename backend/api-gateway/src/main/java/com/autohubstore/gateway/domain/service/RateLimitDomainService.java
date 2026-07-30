package com.autohubstore.gateway.domain.service;

import com.autohubstore.gateway.domain.port.in.CheckRateLimitUseCase;
import com.autohubstore.gateway.domain.port.out.RateLimitPort;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class RateLimitDomainService implements CheckRateLimitUseCase {

    private static final int PUBLIC_LIMIT = 100;
    private static final int AUTH_LIMIT = 200;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final RateLimitPort rateLimitPort;

    public RateLimitDomainService(final RateLimitPort rateLimitPort) {
        this.rateLimitPort = rateLimitPort;
    }

    @Override
    public Mono<Boolean> isAllowed(final String clientKey, final boolean authenticated) {
        final int limit = authenticated ? AUTH_LIMIT : PUBLIC_LIMIT;
        final String redisKey = "ratelimit:" + clientKey;

        return rateLimitPort.increment(redisKey)
                .flatMap(count -> {
                    if (count == 1L) {
                        return rateLimitPort.setExpiry(redisKey, TTL).thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                });
    }

}
