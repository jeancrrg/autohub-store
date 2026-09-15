package com.autohubstore.userservice.unit.service;

import com.autohubstore.userservice.domain.service.PasswordDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordDomainServiceTest {

    private static final String RAW_PASSWORD = "senha-secreta-123";
    private static final String OTHER_PASSWORD = "outra-senha-456";

    private PasswordDomainService passwordDomainService;

    @BeforeEach
    void setUp() {
        passwordDomainService = new PasswordDomainService();
    }

    @Test
    @DisplayName("Deve gerar hash diferente do texto original quando codificar a senha")
    void shouldGenerateHashDifferentFromRawPassword() {
        String hash = passwordDomainService.hash(RAW_PASSWORD);

        assertThat(hash).isNotEqualTo(RAW_PASSWORD);
        assertThat(hash).isNotBlank();
    }

    @Test
    @DisplayName("Deve confirmar correspondencia quando a senha em texto plano bater com o hash")
    void shouldMatchWhenRawPasswordCorrespondsToHash() {
        String hash = passwordDomainService.hash(RAW_PASSWORD);

        assertThat(passwordDomainService.matches(RAW_PASSWORD, hash)).isTrue();
    }

    @Test
    @DisplayName("Deve negar correspondencia quando a senha em texto plano nao bater com o hash")
    void shouldNotMatchWhenRawPasswordDoesNotCorrespondToHash() {
        String hash = passwordDomainService.hash(RAW_PASSWORD);

        assertThat(passwordDomainService.matches(OTHER_PASSWORD, hash)).isFalse();
    }

}
