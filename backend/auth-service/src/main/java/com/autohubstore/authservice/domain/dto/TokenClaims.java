package com.autohubstore.authservice.domain.dto;

import java.util.List;
import java.util.UUID;

public record TokenClaims(String jti, UUID userId, String email, List<String> roles) {

}
