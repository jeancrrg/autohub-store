package com.autohubstore.authservice.infrastructure.external;

import com.autohubstore.authservice.application.dto.request.UpdatePasswordRequest;
import com.autohubstore.authservice.application.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.application.dto.response.ExistsResponse;
import com.autohubstore.authservice.application.dto.response.UserCredentialsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

/**
 * Feign client para o User Service (porta 8003).
 * Comunica-se internamente via service name ou URL configurável.
 */
@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8003}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/email/{email}")
    UserCredentialsResponse findByEmail(@PathVariable("email") String email);

    @GetMapping("/api/v1/users/exists/email/{email}")
    ExistsResponse existsByEmail(@PathVariable("email") String email);

    @PostMapping("/api/v1/users/validate-credentials")
    UserCredentialsResponse validateCredentials(@RequestBody ValidateCredentialsRequest request);

    @PutMapping("/api/v1/users/{userId}/password")
    void updatePassword(@PathVariable("userId") UUID userId,
                        @RequestBody UpdatePasswordRequest request);

}
