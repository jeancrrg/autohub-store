package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.request.CreateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;

public interface CreateUserUseCase {

    UserResponse createUser(CreateUserRequest request);

}
