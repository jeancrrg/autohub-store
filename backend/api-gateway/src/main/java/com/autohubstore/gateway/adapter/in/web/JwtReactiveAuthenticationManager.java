package com.autohubstore.gateway.adapter.in.web;

import com.autohubstore.gateway.domain.model.JwtClaims;
import com.autohubstore.gateway.domain.port.in.ValidateTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ValidateTokenUseCase validateTokenUseCase;

    @Override
    public Mono<Authentication> authenticate(final Authentication authentication) {
        final String token = (String) authentication.getCredentials();
        return Mono.fromCallable(() -> validateTokenUseCase.validate(token))
                .map(claims -> buildAuthentication(claims, token))
                .onErrorMap(e -> new BadCredentialsException("Invalid JWT: " + e.getMessage()));
    }

    private Authentication buildAuthentication(final JwtClaims claims, final String token) {
        final List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(claims, token, authorities);
    }

}
