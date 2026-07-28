package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.dto.RefreshRequest;
import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;

/**
 * Input boundary: valida refresh token, rotaciona e emite novo par de tokens.
 */
public class RefreshTokenUseCase {

    private final TokenDomainService tokenDomainService;
    private final UserServicePort userServicePort;
    private final JwtPort jwtPort;
    private final long accessTokenTtlSeconds;

    public RefreshTokenUseCase(TokenDomainService tokenDomainService,
                               UserServicePort userServicePort,
                               JwtPort jwtPort,
                               long accessTokenTtlSeconds) {
        this.tokenDomainService = tokenDomainService;
        this.userServicePort = userServicePort;
        this.jwtPort = jwtPort;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public LoginResponse execute(RefreshRequest request) {
        RefreshToken newToken = tokenDomainService.rotateRefreshToken(request.refreshToken());

        UserServicePort.UserCredentials credentials =
                userServicePort.findByEmail(findEmailByUserId(newToken));

        String accessToken = jwtPort.generateAccessToken(
                credentials.userId(), credentials.email(), credentials.roles());

        return LoginResponse.of(accessToken, newToken.getToken(), accessTokenTtlSeconds);
    }

    // O User Service é a fonte da verdade; buscamos os dados atualizados pelo userId
    private String findEmailByUserId(RefreshToken token) {
        // Passamos o userId como string ao Feign — o UserServicePort resolve o e-mail
        return token.getUserId().toString();
    }
}
