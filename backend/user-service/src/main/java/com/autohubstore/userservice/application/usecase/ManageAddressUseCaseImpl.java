package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.request.AddressRequest;
import com.autohubstore.userservice.application.dto.response.AddressResponse;
import com.autohubstore.userservice.application.mapper.AddressMapper;
import com.autohubstore.userservice.domain.model.Address;
import com.autohubstore.userservice.domain.repository.AddressRepository;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageAddressUseCaseImpl implements ManageAddressUseCase {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> findAddresses(UUID userId) {
        ensureUserExists(userId);
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        ensureUserExists(userId);

        if (request.isDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = addressMapper.toDomain(request);
        address.setUserId(userId);

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
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
