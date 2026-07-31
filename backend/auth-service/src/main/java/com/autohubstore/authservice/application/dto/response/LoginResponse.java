package com.autohubstore.authservice.application.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }

}
