package com.autohubstore.userservice.domain.dto.response;

import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.domain.enums.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                List.of(user.getRole()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
