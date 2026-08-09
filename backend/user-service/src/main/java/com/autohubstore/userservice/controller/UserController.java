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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findUser(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(id, request));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(@PathVariable String email) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserByEmail(email));
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<ExistsResponse> findExistsByEmail(@PathVariable String email) {
        return ResponseEntity.status(HttpStatus.OK).body(new ExistsResponse(userService.existsByEmail(email)));
    }

    @PostMapping("/validate-credentials")
    public ResponseEntity<UserResponse> validateCredentials(
            @Valid @RequestBody ValidateCredentialsRequest request) {
        UserResponse response = userService.validateCredentials(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(id, request.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
