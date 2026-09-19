package com.autohubstore.catalogservice.messaging;

import com.autohubstore.catalogservice.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductChangedEvent(
        UUID productId,
        String sku,
        String slug,
        String name,
        String description,
        BigDecimal price,
        UUID categoryId,
        String categoryName,
        ProductStatus status
) {}
