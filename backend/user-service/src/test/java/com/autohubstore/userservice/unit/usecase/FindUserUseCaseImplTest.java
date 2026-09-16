package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.application.usecase.FindUserUseCaseImpl;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserUseCaseImplTest {

    private static final String USER_EMAIL = "cliente@autohubstore.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FindUserUseCaseImpl findUserUseCase;

    @Test
    @DisplayName("Deve retornar usuario quando o id existir")
    void shouldReturnUserWhenIdExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("Nome Completo").email(USER_EMAIL).build();
        UserResponse response = new UserResponse(userId, USER_EMAIL, "Nome Completo", null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = findUserUseCase.findUserById(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve lancar excecao quando o id nao for encontrado")
    void shouldThrowExceptionWhenIdIsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> findUserUseCase.findUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar usuario quando o email existir")
    void shouldReturnUserWhenEmailExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("Nome Completo").email(USER_EMAIL).build();
        UserResponse response = new UserResponse(userId, USER_EMAIL, "Nome Completo", null, null, null, null);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = findUserUseCase.findUserByEmail(USER_EMAIL);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve lancar excecao quando o email nao for encontrado")
    void shouldThrowExceptionWhenEmailIsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> findUserUseCase.findUserByEmail(USER_EMAIL))
                .isInstanceOf(UserNotFoundException.class);
    }

}
