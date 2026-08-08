package com.autohubstore.catalogservice.domain.dto.response;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug
) {}
