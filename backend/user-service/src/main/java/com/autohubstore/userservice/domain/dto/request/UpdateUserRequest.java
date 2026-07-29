package com.autohubstore.userservice.domain.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 255) String fullName
) {}
