package com.autohubstore.catalogservice.domain.dto.response;

import com.autohubstore.catalogservice.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String slug,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String brandSlug,
        ProductStatus status,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {}
