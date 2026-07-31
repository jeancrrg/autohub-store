package com.autohubstore.catalogservice.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado no Kafka a cada visualização de detalhes de produto.
 * Tópico: catalog.product-viewed
 */
public record ProductViewedEvent(
        UUID productId,
        String productName,
        Instant viewedAt
) {}
