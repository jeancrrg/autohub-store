package com.autohubstore.userservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventPublisher {

    private static final String TOPIC_USER_CREATED = "user.created";

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public void publishUserCreated(UserCreatedEvent event) {
        kafkaTemplate.send(TOPIC_USER_CREATED, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar UserCreatedEvent userId={}", event.userId(), ex);
                    }
                    else {
                        log.info("UserCreatedEvent publicado: userId={} topic={}",
                                event.userId(), TOPIC_USER_CREATED);
                    }
                });
    }

}
