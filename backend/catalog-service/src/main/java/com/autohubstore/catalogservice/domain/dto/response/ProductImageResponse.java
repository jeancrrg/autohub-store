package com.autohubstore.catalogservice.domain.dto.response;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        boolean primary
) {}
