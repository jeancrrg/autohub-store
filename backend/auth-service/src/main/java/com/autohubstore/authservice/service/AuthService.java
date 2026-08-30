package com.autohubstore.authservice.service;

import com.autohubstore.authservice.client.UserServiceClient;
import com.autohubstore.authservice.domain.dto.TokenClaims;
import com.autohubstore.authservice.domain.dto.request.ForgotPasswordRequest;
import com.autohubstore.authservice.domain.dto.request.LoginRequest;
import com.autohubstore.authservice.domain.dto.request.ResetPasswordRequest;
import com.autohubstore.authservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.domain.dto.response.LoginResponse;
import com.autohubstore.authservice.domain.dto.response.UserVerificationResponse;
import com.autohubstore.authservice.domain.entity.PasswordResetToken;
import com.autohubstore.authservice.domain.entity.RefreshToken;
import com.autohubstore.authservice.domain.event.PasswordResetRequestedEvent;
import com.autohubstore.authservice.exception.InactiveAccountException;
import com.autohubstore.authservice.exception.InvalidCredentialsException;
import com.autohubstore.authservice.exception.InvalidTokenException;
import com.autohubstore.authservice.messaging.PasswordResetEventPublisher;

import feign.FeignException;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final UserServiceClient userServiceClient;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetEventPublisher passwordResetEventPublisher;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessTokenTtlMs;

    public LoginResponse login(LoginRequest request) {
        UserVerificationResponse user = verifyCredentials(request.email(), request.password());

        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), List.of(user.role()));
        RefreshToken refreshToken = tokenService.createRefreshToken(user.id());

        return LoginResponse.of(accessToken, refreshToken.getToken(), accessTokenTtlSeconds());
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            TokenClaims claims = jwtService.extractClaims(accessToken);
            long remainingTtl = jwtService.getRemainingTtlSeconds(accessToken);
            if (remainingTtl > 0) {
                tokenBlacklistService.blacklist(claims.jti(), remainingTtl);
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }

    public LoginResponse refresh(String refreshToken) {
        RefreshToken newToken = tokenService.rotateRefreshToken(refreshToken);
        UserVerificationResponse user = findUserById(newToken.getUserId());

        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), List.of(user.role()));

        return LoginResponse.of(accessToken, newToken.getToken(), accessTokenTtlSeconds());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        UserVerificationResponse user;
        try {
            user = userServiceClient.findUserByEmail(request.email());
        } catch (FeignException.NotFound e) {
            return;
        }

        PasswordResetToken resetToken = tokenService.createPasswordResetToken(user.id());

        passwordResetEventPublisher.publishPasswordResetRequested(
                new PasswordResetRequestedEvent(
                        user.id(),
                        user.email(),
                        resetToken.getToken(),
                        resetToken.getExpiresAt()
                )
        );
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenService.consumePasswordResetToken(request.token());
        userServiceClient.updatePassword(token.getUserId(), Map.of("newPassword", request.newPassword()));
    }

    private UserVerificationResponse verifyCredentials(String email, String password) {
        try {
            return userServiceClient.verifyCredentials(new ValidateCredentialsRequest(email, password));
        } catch (FeignException.Unauthorized e) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        } catch (FeignException.Forbidden e) {
            throw new InactiveAccountException("Conta inativa ou bloqueada");
        }
    }

    private UserVerificationResponse findUserById(UUID userId) {
        try {
            return userServiceClient.findUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new InvalidTokenException("Usuário do refresh token não encontrado");
        }
    }

    private long accessTokenTtlSeconds() {
        return accessTokenTtlMs / MILLIS_PER_SECOND;
    }

}
