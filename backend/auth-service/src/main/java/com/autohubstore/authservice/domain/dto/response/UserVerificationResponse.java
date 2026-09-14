package com.autohubstore.authservice.domain.dto.response;

import java.util.UUID;

public record UserVerificationResponse(UUID id, String email, String role) {

}
