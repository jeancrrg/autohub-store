package com.autohubstore.userservice.domain.dto.response;

import com.autohubstore.userservice.domain.entity.Address;

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
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }
}
