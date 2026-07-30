package com.autohubstore.gateway.adapter.in.web;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;

@Component
public class ServiceRouteFactory {

    public RouteLocatorBuilder.Builder apply(final RouteLocatorBuilder.Builder builder, final ServiceRouteDefinition definition) {
        return builder.route(definition.serviceId(),
                route -> route.path(definition.pathPrefix())
                        .filters(filter -> filter
                                .addResponseHeader("X-Gateway-Route", definition.serviceId())
                                .circuitBreaker(config -> config
                                        .setName(definition.circuitBreakerName())
                                        .setFallbackUri("forward:/fallback/" + definition.fallbackName())))
                        .uri(definition.serviceUrl()));
    }

}
