package com.autohubstore.userservice.service;

import com.autohubstore.userservice.domain.dto.TokenClaims;
import com.autohubstore.userservice.domain.dto.request.ForgotPasswordRequest;
import com.autohubstore.userservice.domain.dto.request.LoginRequest;
import com.autohubstore.userservice.domain.dto.request.ResetPasswordRequest;
import com.autohubstore.userservice.domain.dto.response.LoginResponse;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.domain.entity.PasswordResetToken;
import com.autohubstore.userservice.domain.entity.RefreshToken;
import com.autohubstore.userservice.domain.event.PasswordResetRequestedEvent;
import com.autohubstore.userservice.messaging.PasswordResetEventPublisher;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetEventPublisher passwordResetEventPublisher;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessTokenTtlMs;

    public LoginResponse login(final LoginRequest request) {
        UserResponse user = userService.validateCredentials(request.email(), request.password());

        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), List.of(user.role().name()));
        RefreshToken refreshToken = tokenService.createRefreshToken(user.id());

        return LoginResponse.of(accessToken, refreshToken.getToken(), accessTokenTtlSeconds());
    }

    public void logout(final String accessToken, final String refreshToken) {
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

    public LoginResponse refresh(final String refreshToken) {
        RefreshToken newToken = tokenService.rotateRefreshToken(refreshToken);
        UserResponse user = userService.findUser(newToken.getUserId());

        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), List.of(user.role().name()));

        return LoginResponse.of(accessToken, newToken.getToken(), accessTokenTtlSeconds());
    }

    public void forgotPassword(final ForgotPasswordRequest request) {
        if (!userService.existsByEmail(request.email())) {
            return;
        }

        UserResponse user = userService.findUserByEmail(request.email());
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

    public void resetPassword(final ResetPasswordRequest request) {
        PasswordResetToken token = tokenService.consumePasswordResetToken(request.token());
        userService.updatePassword(token.getUserId(), request.newPassword());
    }

    private long accessTokenTtlSeconds() {
        return accessTokenTtlMs / MILLIS_PER_SECOND;
    }

}
