package com.autohubstore.authservice.domain.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {

    private static final String BEARER = "Bearer";

    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn) {
        return new LoginResponse(accessToken, refreshToken, BEARER, expiresIn, refreshExpiresIn);
    }

}
