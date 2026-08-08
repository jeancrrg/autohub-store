package com.autohubstore.catalogservice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "Slug é obrigatório")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug deve conter apenas letras minúsculas, números e hífens")
        String slug
) {}
