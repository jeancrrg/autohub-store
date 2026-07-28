package com.autohubstore.authservice.infrastructure.security;

import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtPort jwtPort;
    private final TokenBlacklistPort tokenBlacklistPort;

    public JwtAuthenticationFilter(JwtPort jwtPort, TokenBlacklistPort tokenBlacklistPort) {
        this.jwtPort = jwtPort;
        this.tokenBlacklistPort = tokenBlacklistPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            if (jwtPort.isTokenExpired(token)) {
                chain.doFilter(request, response);
                return;
            }

            JwtPort.TokenClaims claims = jwtPort.extractClaims(token);

            if (tokenBlacklistPort.isBlacklisted(claims.jti())) {
                chain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.userId(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception ignored) {
            // Token inválido — deixa o SecurityContext vazio para o filtro de segurança tratar
        }

        chain.doFilter(request, response);
    }
}
