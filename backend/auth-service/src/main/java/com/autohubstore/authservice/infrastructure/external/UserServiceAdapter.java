package com.autohubstore.authservice.infrastructure.external;

import com.autohubstore.authservice.application.dto.UserCredentials;
import com.autohubstore.authservice.application.dto.request.UpdatePasswordRequest;
import com.autohubstore.authservice.application.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.application.dto.response.UserCredentialsResponse;
import com.autohubstore.authservice.application.port.UserServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceAdapter implements UserServicePort {

    private final UserServiceClient client;

    @Override
    public UserCredentials validateCredentials(String email, String rawPassword) {
        UserCredentialsResponse response =
                client.validateCredentials(new ValidateCredentialsRequest(email, rawPassword));
        return new UserCredentials(response.userId(), response.email(), response.roles());
    }

    @Override
    public void updatePassword(UUID userId, String newPassword) {
        client.updatePassword(userId, new UpdatePasswordRequest(newPassword));
    }

    @Override
    public boolean existsByEmail(String email) {
        return client.existsByEmail(email).exists();
    }

    @Override
    public UserCredentials findByEmail(String email) {
        UserCredentialsResponse response = client.findByEmail(email);
        return new UserCredentials(response.userId(), response.email(), response.roles());
    }

}
