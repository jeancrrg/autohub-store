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

    public JwtValidationService(String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public JwtClaims validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        return new JwtClaims(userId, email, roles != null ? roles : List.of());
    }

    @Override
    public boolean isValid(String token) {
        try {
            validate(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
