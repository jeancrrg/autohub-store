package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.request.ForgotPasswordRequest;
import com.autohubstore.authservice.application.dto.request.LoginRequest;
import com.autohubstore.authservice.application.dto.response.LoginResponse;
import com.autohubstore.authservice.application.dto.request.RefreshRequest;
import com.autohubstore.authservice.application.dto.request.ResetPasswordRequest;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import com.autohubstore.authservice.infrastructure.web.docs.AuthControllerDocs;

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

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AuthCookieFactory cookieFactory;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = loginUseCase.execute(request);
        return withSessionCookies(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "access_token", required = false) String accessToken,
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        logoutUseCase.execute(accessToken, refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredRefreshTokenCookie().toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "refresh_token", required = true) String refreshToken) {
        LoginResponse tokens = refreshTokenUseCase.execute(new RefreshRequest(refreshToken));
        return withSessionCookies(tokens);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<Void> withSessionCookies(LoginResponse tokens) {
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildAccessTokenCookie(tokens.accessToken(), tokens.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildRefreshTokenCookie(tokens.refreshToken()).toString())
                .build();
    }

}
