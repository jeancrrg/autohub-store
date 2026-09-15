package com.autohubstore.userservice.infrastructure.config;

import com.autohubstore.userservice.domain.service.PasswordDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public PasswordDomainService passwordDomainService() {
        return new PasswordDomainService();
    }

}
