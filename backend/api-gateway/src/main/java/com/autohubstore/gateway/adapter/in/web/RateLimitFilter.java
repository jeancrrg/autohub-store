package com.autohubstore.gateway.adapter.in.web;

import com.autohubstore.gateway.domain.port.in.CheckRateLimitUseCase;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RETRY_AFTER_SECONDS = "60";
    private static final String UNKNOWN_CLIENT = "unknown";

    private final CheckRateLimitUseCase checkRateLimitUseCase;

    public RateLimitFilter(final CheckRateLimitUseCase checkRateLimitUseCase) {
        this.checkRateLimitUseCase = checkRateLimitUseCase;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        final String path = exchange.getRequest().getPath().value();
        final String clientIp = resolveClientIp(exchange);

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> buildAuthenticatedKey(auth, path))
                .defaultIfEmpty(buildAnonymousKey(clientIp, path))
                .flatMap(rlKey -> checkRateLimitUseCase.isAllowed(rlKey.key(), rlKey.authenticated()))
                .flatMap(allowed -> allowed ? chain.filter(exchange) : rejectRequest(exchange));
    }

    private String resolveClientIp(final ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse(UNKNOWN_CLIENT);
    }

    private RateLimitKey buildAuthenticatedKey(final Authentication auth, final String path) {
        return new RateLimitKey(auth.getName() + ":" + path, auth.isAuthenticated());
    }

    private RateLimitKey buildAnonymousKey(final String clientIp, final String path) {
        return new RateLimitKey(clientIp + ":" + path, false);
    }

    private Mono<Void> rejectRequest(final ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add(RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS);
        return exchange.getResponse().setComplete();
    }

}
