package com.autohubstore.gateway.domain.model;

import java.util.List;

public record JwtClaims(String userId, String email, List<String> roles) {

}
