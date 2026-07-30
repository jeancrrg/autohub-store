package com.autohubstore.authservice.infrastructure.config;

import com.autohubstore.authservice.application.port.EventPublisherPort;
import com.autohubstore.authservice.application.port.JwtPort;
import com.autohubstore.authservice.application.port.TokenBlacklistPort;
import com.autohubstore.authservice.application.port.UserServicePort;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import com.autohubstore.authservice.domain.repository.PasswordResetTokenRepository;
import com.autohubstore.authservice.domain.repository.RefreshTokenRepository;
import com.autohubstore.authservice.domain.service.TokenDomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring da Clean Architecture: instancia domain service e use cases sem @Service,
 * injetando as implementações de infraestrutura via interfaces.
 */
@Configuration
public class ApplicationConfig {

    private static final long MILLIS_PER_SECOND = 1000L;

    @Bean
    public TokenDomainService tokenDomainService(
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenTtlMs,
            @Value("${auth.password-reset-ttl-minutes:15}") long passwordResetTtlMinutes) {
        final long refreshTokenTtlSeconds = refreshTokenTtlMs / MILLIS_PER_SECOND;
        return new TokenDomainService(
                refreshTokenRepository,
                passwordResetTokenRepository,
                refreshTokenTtlSeconds,
                passwordResetTtlMinutes
        );
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserServicePort userServicePort,
            TokenDomainService tokenDomainService,
            JwtPort jwtPort,
            @Value("${jwt.expiration-ms:3600000}") long accessTokenTtlMs) {
        return new LoginUseCase(userServicePort, tokenDomainService, jwtPort, accessTokenTtlMs / MILLIS_PER_SECOND);
    }

    @Bean
    public LogoutUseCase logoutUseCase(
            JwtPort jwtPort,
            TokenBlacklistPort tokenBlacklistPort,
            RefreshTokenRepository refreshTokenRepository) {
        return new LogoutUseCase(jwtPort, tokenBlacklistPort, refreshTokenRepository);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            TokenDomainService tokenDomainService,
            UserServicePort userServicePort,
            JwtPort jwtPort,
            @Value("${jwt.expiration-ms:3600000}") long accessTokenTtlMs) {
        return new RefreshTokenUseCase(tokenDomainService, userServicePort, jwtPort, accessTokenTtlMs / MILLIS_PER_SECOND);
    }

    @Bean
    public ForgotPasswordUseCase forgotPasswordUseCase(
            UserServicePort userServicePort,
            TokenDomainService tokenDomainService,
            EventPublisherPort eventPublisherPort) {
        return new ForgotPasswordUseCase(userServicePort, tokenDomainService, eventPublisherPort);
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(
            TokenDomainService tokenDomainService,
            UserServicePort userServicePort) {
        return new ResetPasswordUseCase(tokenDomainService, userServicePort);
    }

}
