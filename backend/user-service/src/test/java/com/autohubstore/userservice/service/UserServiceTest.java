package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.EmailAlreadyExistsException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.messaging.UserCreatedEvent;
import com.autohubstore.userservice.messaging.UserEventPublisher;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.domain.enums.UserStatus;
import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.domain.mapper.UserMapper;
import com.autohubstore.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        userService = new UserService(userRepository, passwordEncoder, eventPublisher, userMapper);
    }

    @Test
    void createUser_shouldHashPasswordAndPublishEvent() {
        CreateUserRequest request = new CreateUserRequest("user@test.com", "João Silva", "senha123");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.fullName()).isEqualTo("João Silva");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

        // Verifica que o evento foi publicado com os dados corretos
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publishUserCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("user@test.com");
        assertThat(eventCaptor.getValue().fullName()).isEqualTo("João Silva");
    }

    @Test
    void createUser_shouldThrowWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("dup@test.com", "Outro", "senha123");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("dup@test.com");

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishUserCreated(any());
    }

    @Test
    void createUser_shouldStoreBCryptHash() {
        CreateUserRequest request = new CreateUserRequest("hash@test.com", "Test", "minha_senha");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request);

        String storedHash = userCaptor.getValue().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("minha_senha");
        assertThat(passwordEncoder.matches("minha_senha", storedHash)).isTrue();
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void validateCredentials_shouldThrowOnWrongPassword() {
        String rawPassword = "correta";
        User user = User.builder()
                .email("cred@test.com")
                .fullName("Cred User")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("cred@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.validateCredentials("cred@test.com", "errada"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválidas");
    }

    @Test
    void validateCredentials_shouldReturnUserOnValidCredentials() {
        String rawPassword = "senha_certa";
        User user = User.builder()
                .email("ok@test.com")
                .fullName("OK User")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.validateCredentials("ok@test.com", rawPassword);

        assertThat(response.email()).isEqualTo("ok@test.com");
    }

    @Test
    void validateCredentials_shouldThrowWhenUserInactive() {
        User user = User.builder()
                .email("blocked@test.com")
                .fullName("Blocked")
                .passwordHash(passwordEncoder.encode("pass"))
                .build();
        user.setStatus(UserStatus.BLOCKED);
        when(userRepository.findByEmail("blocked@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.validateCredentials("blocked@test.com", "pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativa");
    }

    @Test
    void updatePassword_shouldHashAndSaveNewPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("pw@test.com")
                .fullName("PW User")
                .passwordHash(passwordEncoder.encode("old_pass"))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updatePassword(userId, "new_pass_123");

        assertThat(passwordEncoder.matches("new_pass_123", user.getPasswordHash())).isTrue();
    }
}
