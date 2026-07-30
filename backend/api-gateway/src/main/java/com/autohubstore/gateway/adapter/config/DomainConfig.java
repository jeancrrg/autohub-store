package com.autohubstore.gateway.adapter.config;

import com.autohubstore.gateway.domain.port.in.CheckRateLimitUseCase;
import com.autohubstore.gateway.domain.port.in.ValidateTokenUseCase;
import com.autohubstore.gateway.domain.port.out.RateLimitPort;
import com.autohubstore.gateway.domain.service.JwtValidationService;
import com.autohubstore.gateway.domain.service.RateLimitDomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public ValidateTokenUseCase validateTokenUseCase() {
        return new JwtValidationService(jwtSecret);
    }

    @Bean
    public CheckRateLimitUseCase checkRateLimitUseCase(final RateLimitPort rateLimitPort) {
        return new RateLimitDomainService(rateLimitPort);
    }

}
