package com.autohubstore.authservice.application.port;

import com.autohubstore.authservice.domain.event.PasswordResetRequestedEvent;

/**
 * Port para publicação de eventos de domínio — implementado pelo Kafka producer.
 */
public interface EventPublisherPort {

    void publishPasswordResetRequested(PasswordResetRequestedEvent event);
}
