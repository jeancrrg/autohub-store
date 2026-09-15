package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.usecase.UpdatePasswordUseCaseImpl;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePasswordUseCaseImplTest {

    private static final String NEW_PASSWORD = "nova-senha-123";
    private static final String NEW_HASH = "hash-bcrypt-simulado";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @InjectMocks
    private UpdatePasswordUseCaseImpl updatePasswordUseCase;

    @Test
    @DisplayName("Deve atualizar hash da senha quando o usuario existir")
    void shouldUpdatePasswordHashWhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).passwordHash("hash-antigo").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordDomainService.hash(NEW_PASSWORD)).thenReturn(NEW_HASH);

        updatePasswordUseCase.updatePassword(userId, NEW_PASSWORD);

        assertThat(user.getPasswordHash()).isEqualTo(NEW_HASH);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve lancar excecao quando o usuario nao for encontrado")
    void shouldThrowExceptionWhenUserIsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updatePasswordUseCase.updatePassword(userId, NEW_PASSWORD))
                .isInstanceOf(UserNotFoundException.class);
    }

}
