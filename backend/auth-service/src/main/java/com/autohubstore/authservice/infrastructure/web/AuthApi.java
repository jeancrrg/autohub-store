package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.ForgotPasswordRequest;
import com.autohubstore.authservice.application.dto.LoginRequest;
import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.dto.RefreshRequest;
import com.autohubstore.authservice.application.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "Autenticação e gerenciamento de tokens")
public interface AuthApi {

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica com e-mail/senha e retorna access + refresh token")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga tokens",
               security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String refreshToken);

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotaciona o refresh token e emite novo par de tokens")
    ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request);

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Solicita e-mail de reset de senha")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Confirma reset de senha com token temporário")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

}
