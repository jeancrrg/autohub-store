package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.EmailAlreadyExistsException;
import com.autohubstore.userservice.exception.InactiveAccountException;
import com.autohubstore.userservice.exception.InvalidCredentialsException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.messaging.UserCreatedEvent;
import com.autohubstore.userservice.messaging.UserEventPublisher;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.domain.enums.UserStatus;
import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.domain.mapper.UserMapper;
import com.autohubstore.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        eventPublisher.publishUserCreated(new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        ));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findUser(UUID id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse findUserByEmail(String email) {
        return userMapper.toResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email=" + email)));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        userMapper.updateEntityFromRequest(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Valida credenciais no fluxo de login.
     * Retorna os dados do usuário se as credenciais forem válidas.
     */
    @Transactional(readOnly = true)
    public UserResponse validateCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveAccountException("Conta inativa ou bloqueada");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        return userMapper.toResponse(user);
    }

    /**
     * Atualiza a senha do usuário (chamado pelo fluxo de reset de senha).
     */
    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        User user = findOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

}
