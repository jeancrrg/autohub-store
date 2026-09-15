package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;

import java.util.UUID;

public interface FindUserUseCase {

    UserResponse findUserById(UUID id);

    UserResponse findUserByEmail(String email);

}
