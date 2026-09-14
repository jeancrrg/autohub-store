package com.autohubstore.gateway.filter;

import com.autohubstore.gateway.model.JwtClaims;
import com.autohubstore.gateway.service.JwtService;
import com.autohubstore.gateway.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private static final String BLACKLISTED_TOKEN_MESSAGE = "Invalid JWT: token is blacklisted";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public @NonNull Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return Mono.fromCallable(() -> jwtService.validate(token))
                .flatMap(claims -> rejectIfBlacklisted(claims, token))
                .onErrorMap(e -> !(e instanceof BadCredentialsException),
                        e -> new BadCredentialsException("Invalid JWT: " + e.getMessage()));
    }

    private Mono<Authentication> rejectIfBlacklisted(JwtClaims claims, String token) {
        if (claims.jti() == null) {
            return Mono.just(buildAuthentication(claims, token));
        }
        return tokenBlacklistService.isBlacklisted(claims.jti())
                .flatMap(blacklisted -> blacklisted
                        ? Mono.<Authentication>error(new BadCredentialsException(BLACKLISTED_TOKEN_MESSAGE))
                        : Mono.just(buildAuthentication(claims, token)));
    }

    private Authentication buildAuthentication(JwtClaims claims, String token) {
        List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(claims, token, authorities);
    }

}
