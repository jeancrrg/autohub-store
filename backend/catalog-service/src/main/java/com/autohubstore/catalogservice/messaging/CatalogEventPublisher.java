package com.autohubstore.catalogservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CatalogEventPublisher {

    private static final String TOPIC_PRODUCT_CREATED = "catalog.product-created";
    private static final String TOPIC_PRODUCT_UPDATED = "catalog.product-updated";
    private static final String TOPIC_PRODUCT_VIEWED = "catalog.product-viewed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreated(ProductChangedEvent event) {
        send(TOPIC_PRODUCT_CREATED, event.productId(), event);
    }

    public void publishProductUpdated(ProductChangedEvent event) {
        send(TOPIC_PRODUCT_UPDATED, event.productId(), event);
    }

    public void publishProductViewed(ProductViewedEvent event) {
        send(TOPIC_PRODUCT_VIEWED, event.productId(), event);
    }

    private void send(String topic, UUID key, Object event) {
        kafkaTemplate.send(topic, key.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento no tópico {} key={}", topic, key, ex);
                    }
                    else {
                        log.info("Evento publicado: topic={} key={}", topic, key);
                    }
                });
    }

}
