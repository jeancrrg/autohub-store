package com.autohubstore.userservice.controller;

import com.autohubstore.userservice.controller.docs.UserControllerDocs;
import com.autohubstore.userservice.domain.dto.request.CreateUserRequest;
import com.autohubstore.userservice.domain.dto.request.UpdatePasswordRequest;
import com.autohubstore.userservice.domain.dto.request.UpdateUserRequest;
import com.autohubstore.userservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.userservice.domain.dto.response.ExistsResponse;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Override
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @Override
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<ExistsResponse> existsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(new ExistsResponse(userService.existsByEmail(email)));
    }

    @Override
    @PostMapping("/validate-credentials")
    public ResponseEntity<UserResponse> validateCredentials(
            @Valid @RequestBody ValidateCredentialsRequest request) {
        return ResponseEntity.ok(userService.validateCredentials(request.email(), request.password()));
    }

    @Override
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
