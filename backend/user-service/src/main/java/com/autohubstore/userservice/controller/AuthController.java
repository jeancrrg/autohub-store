package com.autohubstore.userservice.controller;

import com.autohubstore.userservice.controller.docs.AuthControllerDocs;
import com.autohubstore.userservice.domain.dto.request.ForgotPasswordRequest;
import com.autohubstore.userservice.domain.dto.request.LoginRequest;
import com.autohubstore.userservice.domain.dto.request.ResetPasswordRequest;
import com.autohubstore.userservice.domain.dto.response.LoginResponse;
import com.autohubstore.userservice.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final AuthCookieFactory cookieFactory;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody final LoginRequest request) {
        LoginResponse tokens = authService.login(request);
        return withSessionCookies(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "access_token", required = false) final String accessToken,
            @CookieValue(value = "refresh_token", required = false) final String refreshToken) {
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredRefreshTokenCookie().toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "refresh_token", required = true) final String refreshToken) {
        LoginResponse tokens = authService.refresh(refreshToken);
        return withSessionCookies(tokens);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody final ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<Void> withSessionCookies(final LoginResponse tokens) {
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildAccessTokenCookie(tokens.accessToken(), tokens.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildRefreshTokenCookie(tokens.refreshToken()).toString())
                .build();
    }

}
