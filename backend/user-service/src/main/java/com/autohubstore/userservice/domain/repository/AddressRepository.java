package com.autohubstore.userservice.domain.repository;

import com.autohubstore.userservice.domain.model.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(UUID id);

    List<Address> findAllByUserId(UUID userId);

    void clearDefaultByUserId(UUID userId);

    void delete(Address address);

}
