package com.autohubstore.userservice.domain.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordDomainService {

    private final BCryptPasswordEncoder encoder;

    public PasswordDomainService() {
        this.encoder = new BCryptPasswordEncoder();
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

}
