package com.autohubstore.authservice.infrastructure.messaging;

import com.autohubstore.authservice.application.port.EventPublisherPort;
import com.autohubstore.authservice.domain.event.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEventPublisher.class);
    private static final String TOPIC = "auth.password-reset";

    private final KafkaTemplate<String, PasswordResetRequestedEvent> kafkaTemplate;

    public PasswordResetEventPublisher(KafkaTemplate<String, PasswordResetRequestedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        kafkaTemplate.send(TOPIC, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar PasswordResetRequestedEvent para userId={}", event.userId(), ex);
                    } else {
                        log.info("PasswordResetRequestedEvent publicado: userId={} topic={}",
                                event.userId(), TOPIC);
                    }
                });
    }
}
