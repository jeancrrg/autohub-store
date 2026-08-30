package com.autohubstore.authservice.domain.dto.response;

import java.util.UUID;

/**
 * Espelha o formato de {@code UserResponse} do User Service — só os campos que o Auth Service
 * precisa (id, email, role) para emitir o access token. Campos extras do JSON (status, fullName,
 * timestamps) são ignorados na desserialização.
 */
public record UserVerificationResponse(UUID id, String email, String role) {

}
