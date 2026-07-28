package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.ResetPasswordRequest;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;

/**
 * Input boundary: confirma reset de senha com o token temporário.
 */
public class ResetPasswordUseCase {

    private final TokenDomainService tokenDomainService;
    private final UserServicePort userServicePort;

    public ResetPasswordUseCase(TokenDomainService tokenDomainService,
                                UserServicePort userServicePort) {
        this.tokenDomainService = tokenDomainService;
        this.userServicePort = userServicePort;
    }

    public void execute(ResetPasswordRequest request) {
        PasswordResetToken token = tokenDomainService.consumePasswordResetToken(request.token());
        userServicePort.updatePassword(token.getUserId(), request.newPassword());
    }
}
