package com.autohubstore.authservice.messaging;

import com.autohubstore.authservice.domain.event.PasswordResetRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordResetEventPublisher {

    private static final String TOPIC = "user.password-reset";

    private final KafkaTemplate<String, PasswordResetRequestedEvent> passwordResetKafkaTemplate;

    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        passwordResetKafkaTemplate.send(TOPIC, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar PasswordResetRequestedEvent para userId={}", event.userId(), ex);
                    } else {
                        log.info("PasswordResetRequestedEvent publicado: userId={} topic={}", event.userId(), TOPIC);
                    }
                });
    }

}
