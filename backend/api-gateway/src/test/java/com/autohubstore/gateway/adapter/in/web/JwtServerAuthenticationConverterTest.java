package com.autohubstore.gateway.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServerAuthenticationConverterTest {

    private final JwtServerAuthenticationConverter converter = new JwtServerAuthenticationConverter();

    @Test
    void extractsTokenFromAccessTokenCookie() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .cookie(new HttpCookie("access_token", "cookie-jwt-value")));

        Authentication auth = converter.convert(exchange).block();

        assertThat(auth).isNotNull();
        assertThat(auth.getCredentials()).isEqualTo("cookie-jwt-value");
    }

    @Test
    void returnsEmptyWhenNoAccessTokenCookiePresent() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders"));

        Authentication auth = converter.convert(exchange).block();

        assertThat(auth).isNull();
    }

}
