package com.autohubstore.gateway.domain.service;

import com.autohubstore.gateway.domain.model.JwtClaims;
import com.autohubstore.gateway.domain.port.in.ValidateTokenUseCase;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.List;

public class JwtValidationService implements ValidateTokenUseCase {

    private final SecretKey signingKey;

    public JwtValidationService(final String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public JwtClaims validate(final String token) {
        final Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        final String userId = claims.getSubject();
        final String email = claims.get("email", String.class);
        final List<String> roles = extractRoles(claims);

        return new JwtClaims(userId, email, roles);
    }

    private List<String> extractRoles(final Claims claims) {
        final Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    @Override
    public boolean isValid(final String token) {
        try {
            validate(token);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

}