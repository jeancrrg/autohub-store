package com.autohubstore.authservice.application.usecase;

import com.autohubstore.authservice.application.dto.ResetPasswordRequest;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.service.TokenDomainService;
import lombok.RequiredArgsConstructor;

/**
 * Input boundary: confirma reset de senha com o token temporário.
 */
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final TokenDomainService tokenDomainService;
    private final UserServicePort userServicePort;

    public void execute(ResetPasswordRequest request) {
        PasswordResetToken token = tokenDomainService.consumePasswordResetToken(request.token());
        userServicePort.updatePassword(token.getUserId(), request.newPassword());
    }

}
