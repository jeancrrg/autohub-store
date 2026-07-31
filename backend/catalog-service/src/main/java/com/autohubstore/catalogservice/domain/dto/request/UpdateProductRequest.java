package com.autohubstore.catalogservice.domain.dto.request;

import com.autohubstore.catalogservice.domain.enums.ProductStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
        String name,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,

        @DecimalMin(value = "0.0", inclusive = true, message = "Preço não pode ser negativo")
        BigDecimal price,

        @Min(value = 0, message = "Quantidade em estoque não pode ser negativa")
        Integer stockQuantity,

        UUID categoryId,

        ProductStatus status
) {}
