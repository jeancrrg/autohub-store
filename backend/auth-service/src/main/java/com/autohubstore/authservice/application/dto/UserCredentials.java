package com.autohubstore.authservice.application.dto;

import java.util.List;
import java.util.UUID;

public record UserCredentials(UUID userId, String email, List<String> roles) {

}
