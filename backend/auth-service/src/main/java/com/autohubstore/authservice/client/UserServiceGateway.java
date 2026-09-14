package com.autohubstore.authservice.client;

import com.autohubstore.authservice.domain.dto.request.ValidateCredentialsRequest;
import com.autohubstore.authservice.domain.dto.response.UserVerificationResponse;
import com.autohubstore.authservice.exception.UserServiceUnavailableException;

import feign.FeignException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceGateway {

    private static final String USER_SERVICE_INSTANCE = "userService";
    private static final String NEW_PASSWORD_FIELD = "newPassword";

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = USER_SERVICE_INSTANCE, fallbackMethod = "verifyCredentialsFallback")
    @Retry(name = USER_SERVICE_INSTANCE)
    public UserVerificationResponse verifyCredentials(ValidateCredentialsRequest request) {
        return userServiceClient.verifyCredentials(request);
    }

    @CircuitBreaker(name = USER_SERVICE_INSTANCE, fallbackMethod = "findUserByIdFallback")
    @Retry(name = USER_SERVICE_INSTANCE)
    public UserVerificationResponse findUserById(UUID id) {
        return userServiceClient.findUserById(id);
    }

    @CircuitBreaker(name = USER_SERVICE_INSTANCE, fallbackMethod = "findUserByEmailFallback")
    @Retry(name = USER_SERVICE_INSTANCE)
    public UserVerificationResponse findUserByEmail(String email) {
        return userServiceClient.findUserByEmail(email);
    }

    @CircuitBreaker(name = USER_SERVICE_INSTANCE, fallbackMethod = "updatePasswordFallback")
    @Retry(name = USER_SERVICE_INSTANCE)
    public void updatePassword(UUID id, String newPassword) {
        userServiceClient.updatePassword(id, Map.of(NEW_PASSWORD_FIELD, newPassword));
    }

    private UserVerificationResponse verifyCredentialsFallback(
            ValidateCredentialsRequest request, FeignException.Unauthorized cause) {
        throw cause;
    }

    private UserVerificationResponse verifyCredentialsFallback(
            ValidateCredentialsRequest request, FeignException.Forbidden cause) {
        throw cause;
    }

    private UserVerificationResponse verifyCredentialsFallback(ValidateCredentialsRequest request, Throwable cause) {
        log.error("Fallback acionado para verifyCredentials: {}", cause.getMessage(), cause);
        throw new UserServiceUnavailableException("User Service indisponivel para verificar credenciais", cause);
    }

    private UserVerificationResponse findUserByIdFallback(UUID id, FeignException.NotFound cause) {
        throw cause;
    }

    private UserVerificationResponse findUserByIdFallback(UUID id, Throwable cause) {
        log.error("Fallback acionado para findUserById: {}", cause.getMessage(), cause);
        throw new UserServiceUnavailableException("User Service indisponivel para buscar usuario por id", cause);
    }

    private UserVerificationResponse findUserByEmailFallback(String email, FeignException.NotFound cause) {
        throw cause;
    }

    private UserVerificationResponse findUserByEmailFallback(String email, Throwable cause) {
        log.error("Fallback acionado para findUserByEmail: {}", cause.getMessage(), cause);
        throw new UserServiceUnavailableException("User Service indisponivel para buscar usuario por e-mail", cause);
    }

    private void updatePasswordFallback(UUID id, String newPassword, Throwable cause) {
        log.error("Fallback acionado para updatePassword: {}", cause.getMessage(), cause);
        throw new UserServiceUnavailableException("User Service indisponivel para atualizar senha", cause);
    }

}
