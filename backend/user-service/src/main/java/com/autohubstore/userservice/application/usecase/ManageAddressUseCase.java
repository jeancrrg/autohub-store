package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.request.AddressRequest;
import com.autohubstore.userservice.application.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface ManageAddressUseCase {

    List<AddressResponse> findAddresses(UUID userId);

    AddressResponse createAddress(UUID userId, AddressRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

}
