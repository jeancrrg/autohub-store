package com.autohubstore.authservice.client;

import com.autohubstore.authservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.domain.dto.response.UserVerificationResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @PostMapping("/internal/v1/users/verify-credentials")
    UserVerificationResponse verifyCredentials(@RequestBody ValidateCredentialsRequest request);

    @GetMapping("/internal/v1/users/{id}")
    UserVerificationResponse findUserById(@PathVariable UUID id);

    @GetMapping("/internal/v1/users/by-email/{email}")
    UserVerificationResponse findUserByEmail(@PathVariable String email);

    @PutMapping("/internal/v1/users/{id}/password")
    void updatePassword(@PathVariable UUID id, @RequestBody Map<String, String> body);

}
