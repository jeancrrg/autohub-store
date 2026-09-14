package com.autohubstore.authservice.unit.service;

import com.autohubstore.authservice.client.UserServiceGateway;
import com.autohubstore.authservice.domain.dto.TokenClaims;
import com.autohubstore.authservice.domain.dto.request.ForgotPasswordRequest;
import com.autohubstore.authservice.domain.dto.request.LoginRequest;
import com.autohubstore.authservice.domain.dto.request.ResetPasswordRequest;
import com.autohubstore.authservice.domain.dto.response.LoginResponse;
import com.autohubstore.authservice.domain.dto.response.UserVerificationResponse;
import com.autohubstore.authservice.domain.entity.PasswordResetToken;
import com.autohubstore.authservice.domain.entity.RefreshToken;
import com.autohubstore.authservice.domain.mapper.TokenMapper;
import com.autohubstore.authservice.exception.InactiveAccountException;
import com.autohubstore.authservice.exception.InvalidCredentialsException;
import com.autohubstore.authservice.exception.UserServiceUnavailableException;
import com.autohubstore.authservice.messaging.PasswordResetEventPublisher;
import com.autohubstore.authservice.service.AuthService;
import com.autohubstore.authservice.service.JwtService;
import com.autohubstore.authservice.service.TokenBlacklistService;
import com.autohubstore.authservice.service.TokenService;

import feign.FeignException;
import feign.Request;
import feign.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long ACCESS_TOKEN_TTL_MS = 3_600_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "cliente@autohubstore.com";
    private static final String PASSWORD = "senha-correta";
    private static final String ROLE = "CUSTOMER";
    private static final String ACCESS_TOKEN = "access-token-jwt";
    private static final String REFRESH_TOKEN_VALUE = "refresh-token-value";
    private static final String RESET_TOKEN_VALUE = "reset-token-value";
    private static final String NEW_PASSWORD = "nova-senha-123";
    private static final long SIXTY_SECONDS = 60L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final long BLACKLIST_TTL_SECONDS = 3_600L;
    private static final long PASSWORD_RESET_TTL_MINUTES = 15L;

    @Mock
    private UserServiceGateway userServiceGateway;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private PasswordResetEventPublisher passwordResetEventPublisher;

    private TokenMapper tokenMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokenMapper = Mappers.getMapper(TokenMapper.class);
        authService = new AuthService(userServiceGateway, tokenService, jwtService, tokenBlacklistService,
                passwordResetEventPublisher);
        ReflectionTestUtils.setField(authService, "accessTokenTtlMs", ACCESS_TOKEN_TTL_MS);
    }

    @Test
    @DisplayName("Deve efetuar login quando credenciais forem validas")
    void shouldLoginSuccessfullyWhenCredentialsAreValid() {
        UserVerificationResponse user = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        RefreshToken refreshToken =
                tokenMapper.toRefreshToken(USER_ID, REFRESH_TOKEN_VALUE, Instant.now().plusSeconds(SIXTY_SECONDS));
        when(userServiceGateway.verifyCredentials(any())).thenReturn(user);
        when(jwtService.generateAccessToken(eq(USER_ID), eq(EMAIL), anyList())).thenReturn(ACCESS_TOKEN);
        when(tokenService.createRefreshToken(USER_ID)).thenReturn(refreshToken);

        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);
        assertThat(response.expiresIn()).isEqualTo(ACCESS_TOKEN_TTL_MS / MILLIS_PER_SECOND);
        assertThat(response.refreshExpiresIn()).isPositive();
    }

    @Test
    @DisplayName("Deve lancar InvalidCredentialsException quando o User Service rejeitar a senha")
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsRejected() {
        when(userServiceGateway.verifyCredentials(any())).thenThrow(feignExceptionForStatus(HTTP_UNAUTHORIZED));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Deve lancar InactiveAccountException quando a conta estiver inativa")
    void shouldThrowInactiveAccountExceptionWhenAccountIsInactive() {
        when(userServiceGateway.verifyCredentials(any())).thenThrow(feignExceptionForStatus(HTTP_FORBIDDEN));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InactiveAccountException.class);
    }

    @Test
    @DisplayName("Deve propagar UserServiceUnavailableException quando o circuit breaker acionar o fallback")
    void shouldPropagateUserServiceUnavailableExceptionWhenCircuitBreakerFallbackTriggers() {
        when(userServiceGateway.verifyCredentials(any()))
                .thenThrow(new UserServiceUnavailableException("User Service indisponivel", new RuntimeException()));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(UserServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Deve rotacionar o refresh token e emitir novo access token no refresh")
    void shouldRotateRefreshTokenAndIssueNewAccessTokenOnRefresh() {
        RefreshToken newToken =
                tokenMapper.toRefreshToken(USER_ID, "novo-refresh-token", Instant.now().plusSeconds(SIXTY_SECONDS));
        UserVerificationResponse user = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        when(tokenService.rotateRefreshToken(REFRESH_TOKEN_VALUE)).thenReturn(newToken);
        when(userServiceGateway.findUserById(USER_ID)).thenReturn(user);
        when(jwtService.generateAccessToken(eq(USER_ID), eq(EMAIL), anyList())).thenReturn(ACCESS_TOKEN);

        LoginResponse response = authService.refresh(REFRESH_TOKEN_VALUE);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo("novo-refresh-token");
    }

    @Test
    @DisplayName("Deve colocar access token na blacklist e revogar refresh token no logout")
    void shouldBlacklistAccessTokenAndRevokeRefreshTokenOnLogout() {
        TokenClaims claims = new TokenClaims("jti-123", USER_ID, EMAIL, List.of(ROLE));
        when(jwtService.extractClaims(ACCESS_TOKEN)).thenReturn(claims);
        when(jwtService.getRemainingTtlSeconds(ACCESS_TOKEN)).thenReturn(BLACKLIST_TTL_SECONDS);

        authService.logout(ACCESS_TOKEN, REFRESH_TOKEN_VALUE);

        verify(tokenBlacklistService).blacklist("jti-123", BLACKLIST_TTL_SECONDS);
        verify(tokenService).revokeRefreshToken(REFRESH_TOKEN_VALUE);
    }

    @Test
    @DisplayName("Deve ser idempotente ao solicitar forgot-password para e-mail inexistente")
    void shouldBeIdempotentWhenRequestingForgotPasswordForNonexistentEmail() {
        when(userServiceGateway.findUserByEmail(EMAIL)).thenThrow(feignExceptionForStatus(HTTP_NOT_FOUND));

        authService.forgotPassword(new ForgotPasswordRequest(EMAIL));

        verify(tokenService, never()).createPasswordResetToken(any());
        verify(passwordResetEventPublisher, never()).publishPasswordResetRequested(any());
    }

    @Test
    @DisplayName("Deve criar token de reset e publicar evento quando o e-mail existir")
    void shouldCreateResetTokenAndPublishEventWhenEmailExists() {
        UserVerificationResponse user = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        PasswordResetToken resetToken =
                tokenMapper.toPasswordResetToken(USER_ID, RESET_TOKEN_VALUE, PASSWORD_RESET_TTL_MINUTES);
        when(userServiceGateway.findUserByEmail(EMAIL)).thenReturn(user);
        when(tokenService.createPasswordResetToken(USER_ID)).thenReturn(resetToken);

        authService.forgotPassword(new ForgotPasswordRequest(EMAIL));

        verify(passwordResetEventPublisher).publishPasswordResetRequested(any());
    }

    @Test
    @DisplayName("Deve consumir o token de reset e atualizar a senha via User Service")
    void shouldConsumeResetTokenAndUpdatePasswordViaUserService() {
        PasswordResetToken resetToken =
                tokenMapper.toPasswordResetToken(USER_ID, RESET_TOKEN_VALUE, PASSWORD_RESET_TTL_MINUTES);
        when(tokenService.consumePasswordResetToken(RESET_TOKEN_VALUE)).thenReturn(resetToken);

        authService.resetPassword(new ResetPasswordRequest(RESET_TOKEN_VALUE, NEW_PASSWORD));

        verify(userServiceGateway).updatePassword(USER_ID, NEW_PASSWORD);
    }

    private FeignException feignExceptionForStatus(int status) {
        Request request = Request.create(Request.HttpMethod.POST, "/internal/v1/users/verify-credentials",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).request(request).build();
        return FeignException.errorStatus("UserServiceClient#call", response);
    }

}
