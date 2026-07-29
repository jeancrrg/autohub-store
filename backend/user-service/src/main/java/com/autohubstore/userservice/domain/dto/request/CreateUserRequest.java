package com.autohubstore.userservice.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 255) String fullName,
        @NotBlank @Size(min = 8) String password
) {}
