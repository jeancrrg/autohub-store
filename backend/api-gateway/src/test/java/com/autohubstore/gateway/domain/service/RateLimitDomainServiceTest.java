package com.autohubstore.gateway.domain.service;

import com.autohubstore.gateway.domain.port.out.RateLimitPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitDomainServiceTest {

    @Mock
    private RateLimitPort rateLimitPort;

    private RateLimitDomainService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitDomainService(rateLimitPort);
    }

    @Test
    void firstRequest_setsExpiry_isAllowed() {
        when(rateLimitPort.increment("ratelimit:1.2.3.4:/test")).thenReturn(Mono.just(1L));
        when(rateLimitPort.setExpiry(eq("ratelimit:1.2.3.4:/test"), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.isAllowed("1.2.3.4:/test", false))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void publicRequest_withinLimit_isAllowed() {
        when(rateLimitPort.increment("ratelimit:1.2.3.4:/test")).thenReturn(Mono.just(50L));

        StepVerifier.create(service.isAllowed("1.2.3.4:/test", false))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void publicRequest_atLimit_isAllowed() {
        when(rateLimitPort.increment("ratelimit:1.2.3.4:/test")).thenReturn(Mono.just(100L));

        StepVerifier.create(service.isAllowed("1.2.3.4:/test", false))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void publicRequest_overLimit_isBlocked() {
        when(rateLimitPort.increment("ratelimit:1.2.3.4:/test")).thenReturn(Mono.just(101L));

        StepVerifier.create(service.isAllowed("1.2.3.4:/test", false))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void authenticatedRequest_withinLimit_isAllowed() {
        when(rateLimitPort.increment("ratelimit:user-42:/api/v1/orders")).thenReturn(Mono.just(150L));

        StepVerifier.create(service.isAllowed("user-42:/api/v1/orders", true))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void authenticatedRequest_atLimit_isAllowed() {
        when(rateLimitPort.increment("ratelimit:user-42:/api/v1/orders")).thenReturn(Mono.just(200L));

        StepVerifier.create(service.isAllowed("user-42:/api/v1/orders", true))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void authenticatedRequest_overLimit_isBlocked() {
        when(rateLimitPort.increment("ratelimit:user-42:/api/v1/orders")).thenReturn(Mono.just(201L));

        StepVerifier.create(service.isAllowed("user-42:/api/v1/orders", true))
                .expectNext(false)
                .verifyComplete();
    }
}
