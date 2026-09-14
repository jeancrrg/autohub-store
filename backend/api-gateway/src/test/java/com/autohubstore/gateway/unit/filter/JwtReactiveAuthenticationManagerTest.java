package com.autohubstore.gateway.unit.filter;

import com.autohubstore.gateway.filter.JwtReactiveAuthenticationManager;
import com.autohubstore.gateway.model.JwtClaims;
import com.autohubstore.gateway.service.JwtService;
import com.autohubstore.gateway.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtReactiveAuthenticationManagerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String EMAIL = "cliente@autohubstore.com";
    private static final String JTI = "22222222-2222-2222-2222-222222222222";

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Deve autenticar quando o token for valido e nao estiver na blacklist")
    void shouldAuthenticateWhenTokenIsValidAndNotBlacklisted() {
        when(jwtService.validate(TOKEN))
                .thenReturn(new JwtClaims(JTI, USER_ID, EMAIL, List.of("CUSTOMER")));
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(Mono.just(false));

        JwtReactiveAuthenticationManager manager =
                new JwtReactiveAuthenticationManager(jwtService, tokenBlacklistService);
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        StepVerifier.create(manager.authenticate(authentication))
                .expectNextMatches(Authentication::isAuthenticated)
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve rejeitar autenticacao quando o token estiver na blacklist")
    void shouldRejectAuthenticationWhenTokenIsBlacklisted() {
        when(jwtService.validate(TOKEN))
                .thenReturn(new JwtClaims(JTI, USER_ID, EMAIL, List.of("CUSTOMER")));
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(Mono.just(true));

        JwtReactiveAuthenticationManager manager =
                new JwtReactiveAuthenticationManager(jwtService, tokenBlacklistService);
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        StepVerifier.create(manager.authenticate(authentication))
                .expectError(BadCredentialsException.class)
                .verify();
    }

}
