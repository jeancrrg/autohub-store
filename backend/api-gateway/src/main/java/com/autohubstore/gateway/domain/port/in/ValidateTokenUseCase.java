package com.autohubstore.gateway.domain.port.in;

import com.autohubstore.gateway.domain.model.JwtClaims;

public interface ValidateTokenUseCase {

    JwtClaims validate(String token);

    boolean isValid(String token);

}