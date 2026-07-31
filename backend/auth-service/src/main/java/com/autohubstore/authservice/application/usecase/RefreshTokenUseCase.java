package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.response.LoginResponse;
import com.autohubstore.authservice.application.dto.request.RefreshRequest;
import com.autohubstore.authservice.application.dto.UserCredentials;
import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;
import lombok.RequiredArgsConstructor;

/**
 * Input boundary: valida refresh token, rotaciona e emite novo par de tokens.
 */
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final TokenDomainService tokenDomainService;
    private final UserServicePort userServicePort;
    private final JwtPort jwtPort;
    private final long accessTokenTtlSeconds;

    public LoginResponse execute(RefreshRequest request) {
        RefreshToken newToken = tokenDomainService.rotateRefreshToken(request.refreshToken());

        UserCredentials credentials =
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
