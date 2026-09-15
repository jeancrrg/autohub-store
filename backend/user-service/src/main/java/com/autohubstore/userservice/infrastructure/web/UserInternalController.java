package com.autohubstore.userservice.infrastructure.web;

import com.autohubstore.userservice.application.dto.request.UpdatePasswordRequest;
import com.autohubstore.userservice.application.dto.request.ValidateCredentialsRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.usecase.FindUserUseCase;
import com.autohubstore.userservice.application.usecase.UpdatePasswordUseCase;
import com.autohubstore.userservice.application.usecase.VerifyCredentialsUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Internal", description = """
        Endpoints internos, não roteados pelo Gateway — só acessíveis dentro da rede Docker.
        Consumidos pelo Auth Service via OpenFeign para validar/atualizar credencial sem
        replicar o dado de senha fora deste serviço.
        """)
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final VerifyCredentialsUseCase verifyCredentialsUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;

    @PostMapping("/verify-credentials")
    public ResponseEntity<UserResponse> verifyCredentials(
            @Valid @RequestBody ValidateCredentialsRequest request) {
        UserResponse user = verifyCredentialsUseCase.verifyCredentials(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findUserById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(findUserUseCase.findUserById(id));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(@PathVariable String email) {
        return ResponseEntity.status(HttpStatus.OK).body(findUserUseCase.findUserByEmail(email));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable UUID id, @Valid @RequestBody UpdatePasswordRequest request) {
        updatePasswordUseCase.updatePassword(id, request.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
