package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.ForgotPasswordRequest;
import com.autohubstore.authservice.application.port.EventPublisherPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.event.PasswordResetRequestedEvent;
import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;

/**
 * Input boundary: solicita reset de senha — gera token e publica evento Kafka.
 * Retorna sempre 200 para não vazar informações sobre e-mails cadastrados.
 */
public class ForgotPasswordUseCase {

    private final UserServicePort userServicePort;
    private final TokenDomainService tokenDomainService;
    private final EventPublisherPort eventPublisherPort;

    public ForgotPasswordUseCase(UserServicePort userServicePort,
                                 TokenDomainService tokenDomainService,
                                 EventPublisherPort eventPublisherPort) {
        this.userServicePort = userServicePort;
        this.tokenDomainService = tokenDomainService;
        this.eventPublisherPort = eventPublisherPort;
    }

    public void execute(ForgotPasswordRequest request) {
        if (!userServicePort.existsByEmail(request.email())) {
            // Não revelamos se o e-mail existe ou não
            return;
        }

        UserServicePort.UserCredentials user = userServicePort.findByEmail(request.email());
        PasswordResetToken resetToken = tokenDomainService.createPasswordResetToken(user.userId());

        eventPublisherPort.publishPasswordResetRequested(
                new PasswordResetRequestedEvent(
                        user.userId(),
                        user.email(),
                        resetToken.getToken(),
                        resetToken.getExpiresAt()
                )
        );
    }
}
