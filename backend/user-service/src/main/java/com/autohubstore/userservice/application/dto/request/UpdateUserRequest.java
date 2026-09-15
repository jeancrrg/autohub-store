package com.autohubstore.userservice.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 255, message = "O nome completo deve ter entre 2 e 255 caracteres")
        String fullName
) {

}
