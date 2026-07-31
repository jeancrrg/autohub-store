package com.autohubstore.authservice.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieFactoryTest {

    private static final long ACCESS_TTL_SECONDS = 3600L;

    private final AuthCookieFactory factory = new AuthCookieFactory();

    @Test
    void buildAccessTokenCookieIsHttpOnlyAndSecure() {
        ResponseCookie cookie = factory.buildAccessTokenCookie("token-value", ACCESS_TTL_SECONDS);

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(ACCESS_TTL_SECONDS);
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void buildRefreshTokenCookieIsScopedToRefreshPath() {
        ResponseCookie cookie = factory.buildRefreshTokenCookie("refresh-value");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth/refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void expiredCookiesHaveZeroMaxAge() {
        assertThat(factory.expiredAccessTokenCookie().getMaxAge().getSeconds()).isZero();
        assertThat(factory.expiredRefreshTokenCookie().getMaxAge().getSeconds()).isZero();
    }

}
