package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;

public interface VerifyCredentialsUseCase {

    UserResponse verifyCredentials(String email, String rawPassword);

}
