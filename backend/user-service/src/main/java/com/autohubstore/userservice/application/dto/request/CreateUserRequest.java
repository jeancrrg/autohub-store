package com.autohubstore.userservice.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        String email,

        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 2, max = 255, message = "O nome completo deve ter entre 2 e 255 caracteres")
        String fullName,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password
) {

}
