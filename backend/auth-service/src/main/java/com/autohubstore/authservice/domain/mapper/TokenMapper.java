package com.autohubstore.authservice.domain.mapper;

import com.autohubstore.authservice.domain.entity.PasswordResetToken;
import com.autohubstore.authservice.domain.entity.RefreshToken;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TokenMapper {

    int SECONDS_PER_MINUTE = 60;

    default RefreshToken toRefreshToken(UUID userId, String token, Instant expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);
        return refreshToken;
    }

    default PasswordResetToken toPasswordResetToken(UUID userId, String token, long ttlMinutes) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(userId);
        resetToken.setToken(token);
        resetToken.setExpiresAt(Instant.now().plusSeconds(ttlMinutes * SECONDS_PER_MINUTE));
        resetToken.setUsed(false);
        return resetToken;
    }

}
