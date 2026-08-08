package com.autohubstore.catalogservice.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
        String name,

        @Size(max = 50, message = "SKU deve ter no máximo 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "SKU deve conter apenas letras, números e hífens")
        String sku,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Preço não pode ser negativo")
        BigDecimal price,

        @NotNull(message = "Quantidade em estoque é obrigatória")
        @Min(value = 0, message = "Quantidade em estoque não pode ser negativa")
        Integer stockQuantity,

        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @NotNull(message = "Marca é obrigatória")
        UUID brandId
) {}
