package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.application.usecase.UpdateUserUseCaseImpl;
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
class UpdateUserUseCaseImplTest {

    private static final String NEW_FULL_NAME = "Novo Nome Completo";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UpdateUserUseCaseImpl updateUserUseCase;

    @Test
    @DisplayName("Deve atualizar usuario quando o id existir")
    void shouldUpdateUserWhenIdExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("Nome Antigo").build();
        UpdateUserRequest request = new UpdateUserRequest(NEW_FULL_NAME);
        UserResponse response = new UserResponse(userId, "cliente@autohubstore.com", NEW_FULL_NAME,
                null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = updateUserUseCase.updateUser(userId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve lancar excecao quando o usuario nao for encontrado")
    void shouldThrowExceptionWhenUserIsNotFound() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(NEW_FULL_NAME);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateUserUseCase.updateUser(userId, request))
                .isInstanceOf(UserNotFoundException.class);
    }

}
