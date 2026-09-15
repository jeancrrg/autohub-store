package com.autohubstore.userservice.domain.model;

import com.autohubstore.userservice.domain.model.enums.UserRole;
import com.autohubstore.userservice.domain.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private UUID id;

    private String email;

    private String fullName;

    private String passwordHash;

    private UserStatus status;

    private UserRole role;

    private Instant createdAt;

    private Instant updatedAt;

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

}
