package com.autohubstore.gateway.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {

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

    private final ServiceRouteFactory routeFactory;

    @Bean
    public RouteLocator routeLocator(final RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        final List<ServiceRouteDefinition> definitions = createRouteDefinitions();
        for (final ServiceRouteDefinition definition : definitions) {
            routes = routeFactory.apply(routes, definition);
        }
        return routes.build();
    }

    private List<ServiceRouteDefinition> createRouteDefinitions() {
        return List.of(
                new ServiceRouteDefinition("user-service", "/api/v1/users/**", userServiceUrl),
                new ServiceRouteDefinition("user-service-auth", "/api/v1/auth/**", userServiceUrl),
                new ServiceRouteDefinition("catalog-service", "/api/v1/catalog/**", catalogServiceUrl),
                new ServiceRouteDefinition("search-service", "/api/v1/search/**", searchServiceUrl),
                new ServiceRouteDefinition("cart-service", "/api/v1/cart/**", cartServiceUrl),
                new ServiceRouteDefinition("order-service", "/api/v1/orders/**", orderServiceUrl),
                new ServiceRouteDefinition("payment-service", "/api/v1/payments/**", paymentServiceUrl),
                new ServiceRouteDefinition("analytics-service", "/api/v1/analytics/**", analyticsServiceUrl)
        );
    }

}
