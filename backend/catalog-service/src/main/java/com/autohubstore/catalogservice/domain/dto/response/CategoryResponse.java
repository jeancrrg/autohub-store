package com.autohubstore.catalogservice.domain.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        UUID parentId,
        Instant createdAt
) {}
