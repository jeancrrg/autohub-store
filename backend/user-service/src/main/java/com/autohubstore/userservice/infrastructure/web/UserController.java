package com.autohubstore.userservice.infrastructure.web;

import com.autohubstore.userservice.infrastructure.web.docs.UserControllerDocs;
import com.autohubstore.userservice.application.dto.request.CreateUserRequest;
import com.autohubstore.userservice.application.dto.request.UpdatePasswordRequest;
import com.autohubstore.userservice.application.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.usecase.CreateUserUseCase;
import com.autohubstore.userservice.application.usecase.FindUserUseCase;
import com.autohubstore.userservice.application.usecase.UpdatePasswordUseCase;
import com.autohubstore.userservice.application.usecase.UpdateUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final CreateUserUseCase createUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.createUser(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> findCurrentUser() {
        UUID currentUserId = (UUID) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(findUserUseCase.findUserById(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findUser(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(findUserUseCase.findUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(updateUserUseCase.updateUser(id, request));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        updatePasswordUseCase.updatePassword(id, request.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
