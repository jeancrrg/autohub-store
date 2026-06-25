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

    private final CheckRateLimitUseCase checkRateLimitUseCase;

    public RateLimitFilter(CheckRateLimitUseCase checkRateLimitUseCase) {
        this.checkRateLimitUseCase = checkRateLimitUseCase;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String clientIp = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse("unknown");

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> {
                    boolean authenticated = auth.isAuthenticated();
                    String key = authenticated
                            ? auth.getName() + ":" + path
                            : clientIp + ":" + path;
                    return new RateLimitKey(key, authenticated);
                })
                .defaultIfEmpty(new RateLimitKey(clientIp + ":" + path, false))
                .flatMap(rlKey -> checkRateLimitUseCase.isAllowed(rlKey.key(), rlKey.authenticated()))
                .flatMap(allowed -> {
                    if (!allowed) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    private record RateLimitKey(String key, boolean authenticated) {}
}
