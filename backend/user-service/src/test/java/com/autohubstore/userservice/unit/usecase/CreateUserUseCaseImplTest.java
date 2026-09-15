package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.dto.request.CreateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.application.usecase.CreateUserUseCaseImpl;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.model.enums.UserRole;
import com.autohubstore.userservice.domain.model.enums.UserStatus;
import com.autohubstore.userservice.domain.repository.UserEventPublisher;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    private static final String EMAIL = "cliente@autohubstore.com";
    private static final String FULL_NAME = "Cliente Teste";
    private static final String RAW_PASSWORD = "senha12345";
    private static final String HASHED_PASSWORD = "hash-bcrypt-simulado";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CreateUserUseCaseImpl createUserUseCase;

    @Test
    @DisplayName("Deve criar usuario quando o e-mail ainda nao estiver cadastrado")
    void shouldCreateUserWhenEmailIsNotYetRegistered() {
        CreateUserRequest request = new CreateUserRequest(EMAIL, FULL_NAME, RAW_PASSWORD);
        User domainUser = User.builder().email(EMAIL).fullName(FULL_NAME).build();
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName(FULL_NAME)
                .passwordHash(HASHED_PASSWORD)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();
        UserResponse response = new UserResponse(savedUser.getId(), EMAIL, FULL_NAME,
                UserStatus.ACTIVE, UserRole.USER, savedUser.getCreatedAt(), savedUser.getCreatedAt());

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.toDomain(request)).thenReturn(domainUser);
        when(passwordDomainService.hash(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(domainUser)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = createUserUseCase.createUser(request);

        assertThat(result).isEqualTo(response);
        verify(userEventPublisher).publishUserCreated(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando o e-mail ja estiver cadastrado")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(EMAIL, FULL_NAME, RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> createUserUseCase.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(userEventPublisher, never()).publishUserCreated(any());
    }

}
