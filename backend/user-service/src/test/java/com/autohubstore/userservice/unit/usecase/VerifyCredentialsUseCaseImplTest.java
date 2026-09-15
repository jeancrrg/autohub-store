package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.application.usecase.VerifyCredentialsUseCaseImpl;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.model.enums.UserStatus;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.exception.InactiveAccountException;
import com.autohubstore.userservice.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyCredentialsUseCaseImplTest {

    private static final String EMAIL = "cliente@autohubstore.com";
    private static final String RAW_PASSWORD = "senha12345";
    private static final String HASH = "hash-bcrypt-simulado";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VerifyCredentialsUseCaseImpl verifyCredentialsUseCase;

    @Test
    @DisplayName("Deve retornar usuario quando as credenciais forem validas")
    void shouldReturnUserWhenCredentialsAreValid() {
        User user = User.builder().email(EMAIL).passwordHash(HASH).status(UserStatus.ACTIVE).build();
        UserResponse response = new UserResponse(null, EMAIL, null, UserStatus.ACTIVE, null, null, null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordDomainService.matches(RAW_PASSWORD, HASH)).thenReturn(true);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = verifyCredentialsUseCase.verifyCredentials(EMAIL, RAW_PASSWORD);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve lancar excecao quando o e-mail nao estiver cadastrado")
    void shouldThrowExceptionWhenEmailIsNotRegistered() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verifyCredentialsUseCase.verifyCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao quando a conta estiver inativa")
    void shouldThrowExceptionWhenAccountIsInactive() {
        User user = User.builder().email(EMAIL).passwordHash(HASH).status(UserStatus.BLOCKED).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> verifyCredentialsUseCase.verifyCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InactiveAccountException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao quando a senha informada nao corresponder ao hash")
    void shouldThrowExceptionWhenPasswordDoesNotMatchHash() {
        User user = User.builder().email(EMAIL).passwordHash(HASH).status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordDomainService.matches(RAW_PASSWORD, HASH)).thenReturn(false);

        assertThatThrownBy(() -> verifyCredentialsUseCase.verifyCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

}
