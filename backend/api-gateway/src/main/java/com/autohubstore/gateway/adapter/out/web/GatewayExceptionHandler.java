package com.autohubstore.gateway.adapter.out.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_PATH = "path";

    private static final String MSG_UNAUTHORIZED = "Authentication required. Provide a valid Bearer token.";
    private static final String MSG_FORBIDDEN = "Access denied. Insufficient permissions.";
    private static final String MSG_INTERNAL = "An unexpected error occurred. Please try again later.";

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Mono<Void> handle(final ServerWebExchange exchange, final Throwable exception) {
        final HttpStatus status = resolveStatus(exception);
        final String message = resolveMessage(exception, status);
        prepareResponse(exchange, status);
        return writeResponse(exchange, buildResponseBody(exchange, status, message));
    }

    private void prepareResponse(final ServerWebExchange exchange, final HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }

    private Map<String, Object> buildResponseBody(
            final ServerWebExchange exchange, final HttpStatus status, final String message) {

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put(FIELD_STATUS, status.value());
        body.put(FIELD_ERROR, status.getReasonPhrase());
        body.put(FIELD_MESSAGE, message);
        body.put(FIELD_PATH, exchange.getRequest().getPath().value());

        return body;
    }

    private Mono<Void> writeResponse(final ServerWebExchange exchange, final Map<String, Object> body) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                .flatMap(buffer -> exchange.getResponse().writeWith(Mono.just(buffer)));
    }

    private HttpStatus resolveStatus(final Throwable exception) {
        if (exception instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        if (exception instanceof AuthenticationException || exception instanceof JwtException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (exception instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(final Throwable exception, final HttpStatus status) {
        if (exception instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return MSG_UNAUTHORIZED;
        }
        if (status == HttpStatus.FORBIDDEN) {
            return MSG_FORBIDDEN;
        }
        return MSG_INTERNAL;
    }
}