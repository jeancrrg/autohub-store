package com.autohubstore.authservice.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ter um formato válido")
        String email

) {}
