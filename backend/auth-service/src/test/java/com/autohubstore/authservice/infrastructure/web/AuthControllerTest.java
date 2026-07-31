package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.request.LoginRequest;
import com.autohubstore.authservice.application.dto.response.LoginResponse;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final long TTL_SECONDS = 3600L;

    private final LoginUseCase loginUseCase = mock(LoginUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private final ForgotPasswordUseCase forgotPasswordUseCase = mock(ForgotPasswordUseCase.class);
    private final ResetPasswordUseCase resetPasswordUseCase = mock(ResetPasswordUseCase.class);
    private final AuthCookieFactory cookieFactory = new AuthCookieFactory();

    private final AuthController controller = new AuthController(
            loginUseCase, logoutUseCase, refreshTokenUseCase,
            forgotPasswordUseCase, resetPasswordUseCase, cookieFactory);

    @Test
    void loginSetsAccessAndRefreshCookiesAndReturnsEmptyBody() {
        LoginRequest request = new LoginRequest("user@email.com", "password123");
        when(loginUseCase.execute(request))
                .thenReturn(LoginResponse.of("access-jwt", "refresh-jwt", TTL_SECONDS));

        ResponseEntity<Void> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();
        assertThat(setCookies).anyMatch(c -> c.startsWith("access_token=access-jwt"));
        assertThat(setCookies).anyMatch(c -> c.startsWith("refresh_token=refresh-jwt"));
    }

}
