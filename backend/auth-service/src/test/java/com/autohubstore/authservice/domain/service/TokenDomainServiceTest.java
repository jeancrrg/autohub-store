package com.autohubstore.authservice.domain.service;

import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.repository.PasswordResetTokenRepository;
import com.autohubstore.authservice.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenDomainServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private TokenDomainService service;

    private static final long REFRESH_TTL_SECONDS = 604800L; // 7 days
    private static final long RESET_TTL_MINUTES = 15L;

    @BeforeEach
    void setUp() {
        service = new TokenDomainService(
                refreshTokenRepository,
                passwordResetTokenRepository,
                REFRESH_TTL_SECONDS,
                RESET_TTL_MINUTES
        );
    }

    @Test
    void createRefreshToken_shouldRevokeExistingAndCreateNew() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = service.createRefreshToken(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(refreshTokenRepository).save(any());
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotateRefreshToken_shouldRevokeOldAndReturnNew() {
        UUID userId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.create(userId, "old-token",
                Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken newToken = service.rotateRefreshToken("old-token");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(newToken.getToken()).isNotEqualTo("old-token");
        assertThat(newToken.getUserId()).isEqualTo(userId);
    }

    @Test
    void rotateRefreshToken_shouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotateRefreshToken("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void rotateRefreshToken_shouldThrowWhenTokenIsRevoked() {
        UUID userId = UUID.randomUUID();
        RefreshToken revoked = RefreshToken.create(userId, "revoked-token",
                Instant.now().plusSeconds(3600));
        revoked.revoke();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotateRefreshToken("revoked-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void rotateRefreshToken_shouldThrowWhenTokenIsExpired() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = RefreshToken.create(userId, "expired-token",
                Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotateRefreshToken("expired-token"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createPasswordResetToken_shouldPersistWithCorrectTtl() {
        UUID userId = UUID.randomUUID();
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PasswordResetToken token = service.createPasswordResetToken(userId);

        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getExpiresAt())
                .isAfter(Instant.now().plusSeconds(RESET_TTL_MINUTES * 60 - 5))
                .isBefore(Instant.now().plusSeconds(RESET_TTL_MINUTES * 60 + 5));
    }

    @Test
    void consumePasswordResetToken_shouldMarkUsedAndReturn() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.create(userId, "reset-token", RESET_TTL_MINUTES);

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PasswordResetToken consumed = service.consumePasswordResetToken("reset-token");

        assertThat(consumed.isUsed()).isTrue();
    }

    @Test
    void consumePasswordResetToken_shouldThrowWhenAlreadyUsed() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken used = PasswordResetToken.create(userId, "used-token", RESET_TTL_MINUTES);
        used.markUsed();

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.consumePasswordResetToken("used-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já utilizado");
    }
}
