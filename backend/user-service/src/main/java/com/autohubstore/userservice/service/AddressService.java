package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.domain.entity.Address;
import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.repository.AddressRepository;
import com.autohubstore.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        ensureUserExists(userId);
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (request.isDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = new Address(
                user,
                request.street(),
                request.number(),
                request.complement(),
                request.city(),
                request.state(),
                request.zipCode(),
                request.isDefault()
        );

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        ensureUserExists(userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId.toString()));

        if (!address.getUser().getId().equals(userId)) {
            throw new AddressNotFoundException(addressId.toString());
        }

        addressRepository.delete(address);
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }
    }
}
