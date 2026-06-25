package com.autohubstore.gateway.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Value("${services.auth.url:lb://auth-service}")
    private String authServiceUrl;

    @Value("${services.user.url:lb://user-service}")
    private String userServiceUrl;

    @Value("${services.catalog.url:lb://catalog-service}")
    private String catalogServiceUrl;

    @Value("${services.search.url:lb://search-service}")
    private String searchServiceUrl;

    @Value("${services.cart.url:lb://cart-service}")
    private String cartServiceUrl;

    @Value("${services.order.url:lb://order-service}")
    private String orderServiceUrl;

    @Value("${services.payment.url:lb://payment-service}")
    private String paymentServiceUrl;

    @Value("${services.analytics.url:lb://analytics-service}")
    private String analyticsServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service",
                        r -> r.path("/api/v1/auth/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "auth-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("auth-cb")
                                                .setFallbackUri("forward:/fallback/auth")))
                                .uri(authServiceUrl))
                .route("user-service",
                        r -> r.path("/api/v1/users/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "user-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("user-cb")
                                                .setFallbackUri("forward:/fallback/user")))
                                .uri(userServiceUrl))
                .route("catalog-service",
                        r -> r.path("/api/v1/catalog/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "catalog-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("catalog-cb")
                                                .setFallbackUri("forward:/fallback/catalog")))
                                .uri(catalogServiceUrl))
                .route("search-service",
                        r -> r.path("/api/v1/search/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "search-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("search-cb")
                                                .setFallbackUri("forward:/fallback/search")))
                                .uri(searchServiceUrl))
                .route("cart-service",
                        r -> r.path("/api/v1/cart/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "cart-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("cart-cb")
                                                .setFallbackUri("forward:/fallback/cart")))
                                .uri(cartServiceUrl))
                .route("order-service",
                        r -> r.path("/api/v1/orders/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "order-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("order-cb")
                                                .setFallbackUri("forward:/fallback/order")))
                                .uri(orderServiceUrl))
                .route("payment-service",
                        r -> r.path("/api/v1/payments/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "payment-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("payment-cb")
                                                .setFallbackUri("forward:/fallback/payment")))
                                .uri(paymentServiceUrl))
                .route("analytics-service",
                        r -> r.path("/api/v1/analytics/**")
                                .filters(f -> f
                                        .addResponseHeader("X-Gateway-Route", "analytics-service")
                                        .circuitBreaker(cb -> cb
                                                .setName("analytics-cb")
                                                .setFallbackUri("forward:/fallback/analytics")))
                                .uri(analyticsServiceUrl))
                .build();
    }
}
