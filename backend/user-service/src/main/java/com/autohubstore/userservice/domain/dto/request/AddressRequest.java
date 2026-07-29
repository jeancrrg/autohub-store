package com.autohubstore.userservice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Pattern(regexp = "\\d{5}-\\d{3}") String zipCode,
        boolean isDefault
) {}
