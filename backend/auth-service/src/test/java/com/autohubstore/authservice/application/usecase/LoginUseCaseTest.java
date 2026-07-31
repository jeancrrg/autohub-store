package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.request.LoginRequest;
import com.autohubstore.authservice.application.dto.response.LoginResponse;
import com.autohubstore.authservice.application.dto.UserCredentials;
import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.RefreshToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private TokenDomainService tokenDomainService;

    @Mock
    private JwtPort jwtPort;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userServicePort, tokenDomainService, jwtPort, 3600L);
    }

    @Test
    void execute_shouldReturnTokensOnValidCredentials() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        List<String> roles = List.of("USER");

        when(userServicePort.validateCredentials(email, "secret"))
                .thenReturn(new UserCredentials(userId, email, roles));

        when(jwtPort.generateAccessToken(userId, email, roles))
                .thenReturn("access-token-value");

        RefreshToken refreshToken = RefreshToken.create(userId, "refresh-token-value",
                Instant.now().plusSeconds(604800));
        when(tokenDomainService.createRefreshToken(userId)).thenReturn(refreshToken);

        LoginResponse response = useCase.execute(new LoginRequest(email, "secret"));

        assertThat(response.accessToken()).isEqualTo("access-token-value");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void execute_shouldPropagateExceptionOnInvalidCredentials() {
        when(userServicePort.validateCredentials(any(), any()))
                .thenThrow(new IllegalArgumentException("Credenciais inválidas"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                useCase.execute(new LoginRequest("bad@email.com", "wrong"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
