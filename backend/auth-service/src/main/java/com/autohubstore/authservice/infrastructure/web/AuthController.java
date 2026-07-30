package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.ForgotPasswordRequest;
import com.autohubstore.authservice.application.dto.LoginRequest;
import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.dto.RefreshRequest;
import com.autohubstore.authservice.application.dto.ResetPasswordRequest;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final String BEARER_PREFIX = "Bearer ";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    public ResponseEntity<Void> logout(String authHeader, String refreshToken) {
        String accessToken = authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length())
                : null;
        logoutUseCase.execute(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<LoginResponse> refresh(RefreshRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.execute(request));
    }

    public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.accepted().build();
    }

    public ResponseEntity<Void> resetPassword(ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }

}
