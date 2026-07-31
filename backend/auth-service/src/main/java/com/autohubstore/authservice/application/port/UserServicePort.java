package com.autohubstore.authservice.application.port;

import com.autohubstore.authservice.application.dto.UserCredentials;

import java.util.UUID;

/**
 * Port para chamadas ao User Service via OpenFeign.
 */
public interface UserServicePort {

    /**
     * Valida credenciais e retorna dados do usuário.
     * Lança exceção se credenciais inválidas.
     */
    UserCredentials validateCredentials(String email, String rawPassword);

    /**
     * Atualiza senha do usuário (chamado após reset-password).
     */
    void updatePassword(UUID userId, String newPassword);

    /**
     * Verifica se e-mail existe no User Service.
     */
    boolean existsByEmail(String email);

    /**
     * Retorna dados básicos do usuário pelo e-mail.
     */
    UserCredentials findByEmail(String email);

}
