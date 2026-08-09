package com.autohubstore.userservice.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth/refresh";
    private static final long REFRESH_TOKEN_TTL_SECONDS = 604800L;
    private static final int ZERO_MAX_AGE = 0;

    public ResponseCookie buildAccessTokenCookie(final String token, final long ttlSeconds) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
    }

    public ResponseCookie buildRefreshTokenCookie(final String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(REFRESH_TOKEN_TTL_SECONDS))
                .build();
    }

    public ResponseCookie expiredAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ZERO_MAX_AGE))
                .build();
    }

    public ResponseCookie expiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(ZERO_MAX_AGE))
                .build();
    }

}
