package com.autohubstore.authservice.application.dto.response;

import java.util.List;
import java.util.UUID;

public record UserCredentialsResponse(UUID userId, String email, List<String> roles) {

}
