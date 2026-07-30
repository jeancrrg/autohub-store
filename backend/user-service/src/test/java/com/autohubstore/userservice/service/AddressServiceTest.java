package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.domain.entity.Address;
import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.domain.mapper.AddressMapper;
import com.autohubstore.userservice.repository.AddressRepository;
import com.autohubstore.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);
        addressService = new AddressService(addressRepository, userRepository, addressMapper);
    }

    @Test
    void listAddresses_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> addressService.listAddresses(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createAddress_shouldClearDefaultWhenNewIsDefault() {
        UUID userId = UUID.randomUUID();
        AddressRequest request = new AddressRequest("Rua B", "20", null, "Campinas", "SP", "13010-000", true);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(userId, request);

        verify(addressRepository).clearDefaultByUserId(userId);
    }

    @Test
    void createAddress_shouldNotClearDefaultWhenNotDefault() {
        UUID userId = UUID.randomUUID();
        AddressRequest request = new AddressRequest("Rua C", "30", null, "Campinas", "SP", "13010-000", false);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(userId, request);

        verify(addressRepository, never()).clearDefaultByUserId(any());
    }

    @Test
    void deleteAddress_shouldThrowWhenAddressBelongsToDifferentUser() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User otherUser = User.builder()
                .email("other@test.com")
                .fullName("Other")
                .passwordHash("hash")
                .build();
        Address address = Address.builder()
                .userId(otherUser.getId())
                .street("Rua X")
                .number("1")
                .city("SP")
                .state("SP")
                .zipCode("01310-100")
                .isDefault(false)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> addressService.deleteAddress(userId, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }
}
