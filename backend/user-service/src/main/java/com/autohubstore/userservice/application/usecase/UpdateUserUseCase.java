package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;

import java.util.UUID;

public interface UpdateUserUseCase {

    UserResponse updateUser(UUID id, UpdateUserRequest request);

}
