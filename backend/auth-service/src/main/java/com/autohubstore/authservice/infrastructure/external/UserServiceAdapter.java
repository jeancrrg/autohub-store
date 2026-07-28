package com.autohubstore.authservice.infrastructure.external;

import com.autohubstore.authservice.application.port.UserServicePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserServiceAdapter implements UserServicePort {

    private final UserServiceClient client;

    public UserServiceAdapter(UserServiceClient client) {
        this.client = client;
    }

    @Override
    public UserCredentials validateCredentials(String email, String rawPassword) {
        UserServiceClient.UserCredentialsResponse response =
                client.validateCredentials(new UserServiceClient.ValidateCredentialsRequest(email, rawPassword));
        return new UserCredentials(response.userId(), response.email(), response.roles());
    }

    @Override
    public void updatePassword(UUID userId, String newPassword) {
        client.updatePassword(userId, new UserServiceClient.UpdatePasswordRequest(newPassword));
    }

    @Override
    public boolean existsByEmail(String email) {
        return client.existsByEmail(email).exists();
    }

    @Override
    public UserCredentials findByEmail(String email) {
        UserServiceClient.UserCredentialsResponse response = client.findByEmail(email);
        return new UserCredentials(response.userId(), response.email(), response.roles());
    }
}
