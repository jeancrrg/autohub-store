package com.autohubstore.gateway.filter;

import com.autohubstore.gateway.model.JwtClaims;
import com.autohubstore.gateway.service.JwtService;
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

    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return Mono.fromCallable(() -> jwtService.validate(token))
                .map(claims -> buildAuthentication(claims, token))
                .onErrorMap(e -> new BadCredentialsException("Invalid JWT: " + e.getMessage()));
    }

    private Authentication buildAuthentication(JwtClaims claims, String token) {
        List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(claims, token, authorities);
    }

}
