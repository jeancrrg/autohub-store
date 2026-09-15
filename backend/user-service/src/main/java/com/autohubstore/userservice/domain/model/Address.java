package com.autohubstore.userservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    private UUID id;

    private UUID userId;

    private String street;

    private String number;

    private String complement;

    private String city;

    private String state;

    private String zipCode;

    private boolean isDefault;

    private Instant createdAt;

}
