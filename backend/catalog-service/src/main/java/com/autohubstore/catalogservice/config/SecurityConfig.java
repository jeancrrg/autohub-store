package com.autohubstore.catalogservice.config;

import com.autohubstore.catalogservice.exception.SecurityInitializationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * O Catalog Service não valida JWT diretamente — o API Gateway já valida token e role
 * antes de rotear a requisição (endpoints admin exigem role ADMIN no Gateway).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
        catch (Exception e) {
            throw new SecurityInitializationException("Falha ao construir a cadeia de filtros de segurança", e);
        }
    }

}
