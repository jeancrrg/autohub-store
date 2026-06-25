package com.autohubstore.gateway.domain.port.in;

import reactor.core.publisher.Mono;

public interface CheckRateLimitUseCase {
    Mono<Boolean> isAllowed(String clientKey, boolean authenticated);
}
