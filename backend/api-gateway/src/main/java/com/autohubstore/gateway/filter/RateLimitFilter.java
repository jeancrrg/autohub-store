package com.autohubstore.gateway.filter;

import com.autohubstore.gateway.model.RateLimitKey;
import com.autohubstore.gateway.service.RateLimitService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RETRY_AFTER_SECONDS = "60";
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final int FILTER_ORDER_OFFSET = 10;

    private final RateLimitService rateLimitService;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + FILTER_ORDER_OFFSET;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String clientIp = resolveClientIp(exchange);

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> buildAuthenticatedKey(auth, path))
                .defaultIfEmpty(buildAnonymousKey(clientIp, path))
                .flatMap(rlKey -> rateLimitService.isAllowed(rlKey.key(), rlKey.authenticated()))
                .flatMap(allowed -> allowed ? chain.filter(exchange) : rejectRequest(exchange));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse(UNKNOWN_CLIENT);
    }

    private RateLimitKey buildAuthenticatedKey(Authentication auth, String path) {
        return new RateLimitKey(auth.getName() + ":" + path, auth.isAuthenticated());
    }

    private RateLimitKey buildAnonymousKey(String clientIp, String path) {
        return new RateLimitKey(clientIp + ":" + path, false);
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add(RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS);
        return exchange.getResponse().setComplete();
    }

}
