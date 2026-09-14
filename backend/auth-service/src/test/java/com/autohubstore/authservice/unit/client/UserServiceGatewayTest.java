package com.autohubstore.authservice.unit.client;

import com.autohubstore.authservice.client.UserServiceClient;
import com.autohubstore.authservice.client.UserServiceGateway;
import com.autohubstore.authservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.domain.dto.response.UserVerificationResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceGatewayTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "cliente@autohubstore.com";
    private static final String PASSWORD = "senha-correta";
    private static final String ROLE = "CUSTOMER";
    private static final String NEW_PASSWORD = "nova-senha-123";

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserServiceGateway userServiceGateway;

    @Test
    @DisplayName("Deve delegar verificacao de credenciais ao UserServiceClient")
    void shouldDelegateVerifyCredentialsToClient() {
        UserVerificationResponse response = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        ValidateCredentialsRequest request = new ValidateCredentialsRequest(EMAIL, PASSWORD);
        when(userServiceClient.verifyCredentials(request)).thenReturn(response);

        UserVerificationResponse result = userServiceGateway.verifyCredentials(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve delegar busca de usuario por id ao UserServiceClient")
    void shouldDelegateFindUserByIdToClient() {
        UserVerificationResponse response = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        when(userServiceClient.findUserById(USER_ID)).thenReturn(response);

        UserVerificationResponse result = userServiceGateway.findUserById(USER_ID);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve delegar busca de usuario por e-mail ao UserServiceClient")
    void shouldDelegateFindUserByEmailToClient() {
        UserVerificationResponse response = new UserVerificationResponse(USER_ID, EMAIL, ROLE);
        when(userServiceClient.findUserByEmail(EMAIL)).thenReturn(response);

        UserVerificationResponse result = userServiceGateway.findUserByEmail(EMAIL);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Deve delegar atualizacao de senha ao UserServiceClient com o campo newPassword")
    void shouldDelegateUpdatePasswordToClientWithNewPasswordField() {
        userServiceGateway.updatePassword(USER_ID, NEW_PASSWORD);

        verify(userServiceClient).updatePassword(eq(USER_ID), eq(Map.of("newPassword", NEW_PASSWORD)));
    }

}
