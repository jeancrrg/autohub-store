package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.EmailAlreadyExistsException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.messaging.UserCreatedEvent;
import com.autohubstore.userservice.messaging.UserEventPublisher;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.domain.enums.UserStatus;
import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User(
                request.email(),
                request.fullName(),
                passwordEncoder.encode(request.password())
        );
        user = userRepository.save(user);

        eventPublisher.publishUserCreated(new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        ));

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return UserResponse.from(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email=" + email)));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Valida credenciais (chamado pelo Auth Service).
     * Retorna os dados do usuário se as credenciais forem válidas.
     */
    @Transactional(readOnly = true)
    public UserResponse validateCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Conta inativa ou bloqueada");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        return UserResponse.from(user);
    }

    /**
     * Atualiza a senha do usuário (chamado pelo Auth Service após reset).
     */
    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        User user = findOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Retorna roles do usuário como lista (para JWT).
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(UUID userId) {
        User user = findOrThrow(userId);
        return List.of(user.getRole());
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
    }
}
