package com.autohubstore.authservice.unit.service;

import com.autohubstore.authservice.domain.entity.PasswordResetToken;
import com.autohubstore.authservice.domain.entity.RefreshToken;
import com.autohubstore.authservice.domain.mapper.TokenMapper;
import com.autohubstore.authservice.exception.InvalidTokenException;
import com.autohubstore.authservice.repository.PasswordResetTokenRepository;
import com.autohubstore.authservice.repository.RefreshTokenRepository;
import com.autohubstore.authservice.service.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final long REFRESH_TOKEN_TTL_MS = 604_800_000L;
    private static final long PASSWORD_RESET_TTL_MINUTES = 15L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String REFRESH_TOKEN_VALUE = "refresh-token-value";
    private static final String RESET_TOKEN_VALUE = "reset-token-value";
    private static final long SIXTY_SECONDS = 60L;
    private static final long TOLERANCE_SECONDS = 5L;
    private static final long SECONDS_PER_MINUTE = 60L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private TokenMapper tokenMapper;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenMapper = Mappers.getMapper(TokenMapper.class);
        tokenService = new TokenService(refreshTokenRepository, passwordResetTokenRepository, tokenMapper,
                REFRESH_TOKEN_TTL_MS, PASSWORD_RESET_TTL_MINUTES);
    }

    @Test
    @DisplayName("Deve revogar tokens antigos e criar um novo refresh token quando usuario fizer login")
    void shouldRevokeExistingTokensAndCreateNewRefreshTokenOnLogin() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = tokenService.createRefreshToken(USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
        assertThat(created.getUserId()).isEqualTo(USER_ID);
        assertThat(created.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve rotacionar o refresh token quando o token atual for valido")
    void shouldRotateRefreshTokenWhenCurrentTokenIsValid() {
        RefreshToken existing =
                tokenMapper.toRefreshToken(USER_ID, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(SIXTY_SECONDS));
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = tokenService.rotateRefreshToken(REFRESH_TOKEN_VALUE);

        assertThat(existing.isRevoked()).isTrue();
        assertThat(rotated.getUserId()).isEqualTo(USER_ID);
        assertThat(rotated.getToken()).isNotEqualTo(REFRESH_TOKEN_VALUE);
    }

    @Test
    @DisplayName("Deve lancar excecao quando refresh token nao for encontrado")
    void shouldThrowExceptionWhenRefreshTokenIsNotFound() {
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(REFRESH_TOKEN_VALUE))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao quando refresh token estiver revogado ou expirado")
    void shouldThrowExceptionWhenRefreshTokenIsRevokedOrExpired() {
        RefreshToken revoked =
                tokenMapper.toRefreshToken(USER_ID, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(SIXTY_SECONDS));
        revoked.revoke();
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(REFRESH_TOKEN_VALUE))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Deve criar token de redefinicao de senha com TTL de 15 minutos")
    void shouldCreatePasswordResetTokenWithFifteenMinutesTtl() {
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        PasswordResetToken token = tokenService.createPasswordResetToken(USER_ID);

        Instant expectedExpiry = before.plusSeconds(PASSWORD_RESET_TTL_MINUTES * SECONDS_PER_MINUTE);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getExpiresAt()).isAfterOrEqualTo(expectedExpiry.minusSeconds(TOLERANCE_SECONDS));
        assertThat(token.getExpiresAt()).isBeforeOrEqualTo(expectedExpiry.plusSeconds(TOLERANCE_SECONDS));
    }

    @Test
    @DisplayName("Deve consumir token de redefinicao valido e marca-lo como usado")
    void shouldConsumeValidPasswordResetTokenAndMarkAsUsed() {
        PasswordResetToken token = tokenMapper.toPasswordResetToken(USER_ID, RESET_TOKEN_VALUE, PASSWORD_RESET_TTL_MINUTES);
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN_VALUE)).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetToken consumed = tokenService.consumePasswordResetToken(RESET_TOKEN_VALUE);

        assertThat(consumed.isUsed()).isTrue();
    }

    @Test
    @DisplayName("Deve lancar excecao quando token de redefinicao nao for encontrado")
    void shouldThrowExceptionWhenPasswordResetTokenIsNotFound() {
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN_VALUE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.consumePasswordResetToken(RESET_TOKEN_VALUE))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao quando token de redefinicao ja tiver sido usado")
    void shouldThrowExceptionWhenPasswordResetTokenIsAlreadyUsed() {
        PasswordResetToken token = tokenMapper.toPasswordResetToken(USER_ID, RESET_TOKEN_VALUE, PASSWORD_RESET_TTL_MINUTES);
        token.markUsed();
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN_VALUE)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.consumePasswordResetToken(RESET_TOKEN_VALUE))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Deve revogar o refresh token quando ele existir")
    void shouldRevokeRefreshTokenWhenItExists() {
        RefreshToken existing =
                tokenMapper.toRefreshToken(USER_ID, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(SIXTY_SECONDS));
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(existing));

        tokenService.revokeRefreshToken(REFRESH_TOKEN_VALUE);

        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    @DisplayName("Deve ser idempotente quando refresh token a revogar nao existir")
    void shouldBeIdempotentWhenRevokingNonexistentRefreshToken() {
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.empty());

        tokenService.revokeRefreshToken(REFRESH_TOKEN_VALUE);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

}
