package com.autohubstore.userservice.acceptance.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Component
public class HttpTestUtil {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String DEFAULT_ROLE = "CUSTOMER";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";
    private static final long ACCESS_TOKEN_TTL_MILLIS = 900_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public ResultActions executePost(String path, Object body) {
        try {
            return mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar POST em " + path, ex);
        }
    }

    public ResultActions executePut(String path, Object body) {
        try {
            return mockMvc.perform(put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar PUT em " + path, ex);
        }
    }

    public ResultActions executeDelete(String path) {
        try {
            return mockMvc.perform(delete(path));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar DELETE em " + path, ex);
        }
    }

    public ResultActions executePostAuthenticated(String path, Object body, UUID authenticatedUserId) {
        try {
            return mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(buildAccessTokenCookie(authenticatedUserId))
                    .content(jsonMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar POST autenticado em " + path, ex);
        }
    }

    public ResultActions executePutAuthenticated(String path, Object body, UUID authenticatedUserId) {
        try {
            return mockMvc.perform(put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(buildAccessTokenCookie(authenticatedUserId))
                    .content(jsonMapper.writeValueAsString(body)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar PUT autenticado em " + path, ex);
        }
    }

    public ResultActions executeGetAuthenticated(String path, UUID authenticatedUserId) {
        try {
            return mockMvc.perform(get(path)
                    .cookie(buildAccessTokenCookie(authenticatedUserId)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar GET autenticado em " + path, ex);
        }
    }

    public ResultActions executeDeleteAuthenticated(String path, UUID authenticatedUserId) {
        try {
            return mockMvc.perform(delete(path)
                    .cookie(buildAccessTokenCookie(authenticatedUserId)));
        } catch (Exception ex) {
            throw new HttpAcceptanceTestException("Falha ao executar DELETE autenticado em " + path, ex);
        }
    }

    private Cookie buildAccessTokenCookie(UUID authenticatedUserId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant issuedAt = Instant.now();
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(authenticatedUserId.toString())
                .claim(EMAIL_CLAIM, authenticatedUserId + "@autohubstore.com")
                .claim(ROLES_CLAIM, List.of(DEFAULT_ROLE))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(ACCESS_TOKEN_TTL_MILLIS)))
                .signWith(secretKey)
                .compact();
        return new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
    }

}
