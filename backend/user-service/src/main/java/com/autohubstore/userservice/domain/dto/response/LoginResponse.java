package com.autohubstore.userservice.domain.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static LoginResponse of(final String accessToken, final String refreshToken, final long expiresIn) {
        return new LoginResponse(accessToken, refreshToken, BEARER, expiresIn);
    }

}
