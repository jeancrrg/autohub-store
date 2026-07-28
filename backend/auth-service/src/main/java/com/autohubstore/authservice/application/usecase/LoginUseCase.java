package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.LoginRequest;
import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;

/**
 * Input boundary: autentica usuário e emite access token + refresh token.
 */
public class LoginUseCase {

    private final UserServicePort userServicePort;
    private final TokenDomainService tokenDomainService;
    private final JwtPort jwtPort;
    private final long accessTokenTtlSeconds;

    public LoginUseCase(UserServicePort userServicePort,
                        TokenDomainService tokenDomainService,
                        JwtPort jwtPort,
                        long accessTokenTtlSeconds) {
        this.userServicePort = userServicePort;
        this.tokenDomainService = tokenDomainService;
        this.jwtPort = jwtPort;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public LoginResponse execute(LoginRequest request) {
        UserServicePort.UserCredentials credentials =
                userServicePort.validateCredentials(request.email(), request.password());

        String accessToken = jwtPort.generateAccessToken(
                credentials.userId(), credentials.email(), credentials.roles());

        RefreshToken refreshToken = tokenDomainService.createRefreshToken(credentials.userId());

        return LoginResponse.of(accessToken, refreshToken.getToken(), accessTokenTtlSeconds);
    }
}
