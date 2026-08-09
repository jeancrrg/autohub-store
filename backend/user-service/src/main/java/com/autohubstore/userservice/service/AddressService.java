package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.domain.entity.Address;
import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.mapper.AddressMapper;
import com.autohubstore.userservice.repository.AddressRepository;
import com.autohubstore.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public List<AddressResponse> findAddresses(UUID userId) {
        ensureUserExists(userId);
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        ensureUserExists(userId);

        if (request.isDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = addressMapper.toEntity(request);
        address.setUserId(userId);

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        ensureUserExists(userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId.toString()));

        if (!userId.equals(address.getUserId())) {
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
