package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.exception.InactiveAccountException;
import com.autohubstore.userservice.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyCredentialsUseCaseImpl implements VerifyCredentialsUseCase {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciais inválidas";
    private static final String INACTIVE_ACCOUNT_MESSAGE = "Conta inativa ou bloqueada";

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse verifyCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isActive()) {
            throw new InactiveAccountException(INACTIVE_ACCOUNT_MESSAGE);
        }

        if (!passwordDomainService.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        return userMapper.toResponse(user);
    }

}
