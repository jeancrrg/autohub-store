package com.autohubstore.userservice.service;

import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import com.autohubstore.userservice.domain.entity.Address;
import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.domain.entity.User;
import com.autohubstore.userservice.repository.AddressRepository;
import com.autohubstore.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        addressService = new AddressService(addressRepository, userRepository);
    }

    @Test
    void listAddresses_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> addressService.listAddresses(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listAddresses_shouldReturnMappedList() {
        UUID userId = UUID.randomUUID();
        User user = new User("u@test.com", "User", "hash");
        Address address = new Address(user, "Rua A", "10", null, "SP", "SP", "01310-100", true);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findAllByUserId(userId)).thenReturn(List.of(address));

        List<AddressResponse> result = addressService.listAddresses(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).street()).isEqualTo("Rua A");
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    void createAddress_shouldClearDefaultWhenNewIsDefault() {
        UUID userId = UUID.randomUUID();
        User user = new User("u@test.com", "User", "hash");
        AddressRequest request = new AddressRequest("Rua B", "20", null, "Campinas", "SP", "13010-000", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(userId, request);

        verify(addressRepository).clearDefaultByUserId(userId);
    }

    @Test
    void createAddress_shouldNotClearDefaultWhenNotDefault() {
        UUID userId = UUID.randomUUID();
        User user = new User("u@test.com", "User", "hash");
        AddressRequest request = new AddressRequest("Rua C", "30", null, "Campinas", "SP", "13010-000", false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(userId, request);

        verify(addressRepository, never()).clearDefaultByUserId(any());
    }

    @Test
    void deleteAddress_shouldThrowWhenAddressBelongsToDifferentUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User otherUser = new User("other@test.com", "Other", "hash");
        Address address = new Address(otherUser, "Rua X", "1", null, "SP", "SP", "01310-100", false);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> addressService.deleteAddress(userId, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }
}
