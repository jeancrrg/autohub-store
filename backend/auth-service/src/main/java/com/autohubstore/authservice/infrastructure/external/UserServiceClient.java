package com.autohubstore.authservice.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign client para o User Service (porta 8003).
 * Comunica-se internamente via service name ou URL configurável.
 */
@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8003}")
public interface UserServiceClient {

    @PostMapping("/api/v1/users/validate-credentials")
    UserCredentialsResponse validateCredentials(@RequestBody ValidateCredentialsRequest request);

    @GetMapping("/api/v1/users/email/{email}")
    UserCredentialsResponse findByEmail(@PathVariable("email") String email);

    @GetMapping("/api/v1/users/exists/email/{email}")
    ExistsResponse existsByEmail(@PathVariable("email") String email);

    @PutMapping("/api/v1/users/{userId}/password")
    void updatePassword(@PathVariable("userId") UUID userId,
                        @RequestBody UpdatePasswordRequest request);

    record ValidateCredentialsRequest(String email, String password) {}

    record UpdatePasswordRequest(String newPassword) {}

    record ExistsResponse(boolean exists) {}

    record UserCredentialsResponse(UUID userId, String email, List<String> roles) {}
}
