package com.autohubstore.userservice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = ResetPasswordRequest.MIN_PASSWORD_LENGTH,
                message = "Nova senha deve ter no mínimo 8 caracteres")
        String newPassword

) {

    private static final int MIN_PASSWORD_LENGTH = 8;

}
