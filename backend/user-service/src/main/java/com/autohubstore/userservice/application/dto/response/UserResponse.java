package com.autohubstore.userservice.application.dto.response;

import com.autohubstore.userservice.domain.model.enums.UserRole;
import com.autohubstore.userservice.domain.model.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {

}
