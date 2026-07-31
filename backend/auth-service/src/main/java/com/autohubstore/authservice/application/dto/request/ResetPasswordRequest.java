package com.autohubstore.authservice.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = MIN_PASSWORD_LENGTH, message = "Nova senha deve ter no mínimo " + MIN_PASSWORD_LENGTH
                + " caracteres")
        String newPassword

) {

    private static final int MIN_PASSWORD_LENGTH = 8;

}
