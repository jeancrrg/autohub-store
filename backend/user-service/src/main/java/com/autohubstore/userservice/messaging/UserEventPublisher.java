package com.autohubstore.userservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventPublisher.class);
    private static final String TOPIC_USER_CREATED = "user.created";

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, UserCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserCreated(UserCreatedEvent event) {
        kafkaTemplate.send(TOPIC_USER_CREATED, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Falha ao publicar UserCreatedEvent userId={}", event.userId(), ex);
                    }
                    else {
                        LOGGER.info("UserCreatedEvent publicado: userId={} topic={}",
                                event.userId(), TOPIC_USER_CREATED);
                    }
                });
    }
}
