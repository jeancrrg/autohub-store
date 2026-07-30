package com.autohubstore.userservice.domain.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String street,
        String number,
        String complement,
        String city,
        String state,
        String zipCode,
        boolean isDefault,
        Instant createdAt
) {

}
