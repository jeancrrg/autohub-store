package com.autohubstore.userservice.unit.usecase;

import com.autohubstore.userservice.application.dto.request.AddressRequest;
import com.autohubstore.userservice.application.dto.response.AddressResponse;
import com.autohubstore.userservice.application.mapper.AddressMapper;
import com.autohubstore.userservice.application.usecase.ManageAddressUseCaseImpl;
import com.autohubstore.userservice.domain.model.Address;
import com.autohubstore.userservice.domain.repository.AddressRepository;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.exception.AddressNotFoundException;
import com.autohubstore.userservice.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageAddressUseCaseImplTest {

    private static final String ZIP_CODE = "12345-678";

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private ManageAddressUseCaseImpl manageAddressUseCase;

    @Test
    @DisplayName("Deve criar endereco e limpar padrao anterior quando novo endereco for padrao")
    void shouldCreateAddressAndClearPreviousDefaultWhenNewAddressIsDefault() {
        UUID userId = UUID.randomUUID();
        AddressRequest request = new AddressRequest("Rua A", "100", null, "Cidade", "SP", ZIP_CODE, true);
        Address domainAddress = Address.builder().street("Rua A").isDefault(true).build();
        Address savedAddress = Address.builder().id(UUID.randomUUID()).userId(userId).isDefault(true).build();
        AddressResponse response = new AddressResponse(savedAddress.getId(), "Rua A", "100", null,
                "Cidade", "SP", ZIP_CODE, true, null);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressMapper.toDomain(request)).thenReturn(domainAddress);
        when(addressRepository.save(domainAddress)).thenReturn(savedAddress);
        when(addressMapper.toResponse(savedAddress)).thenReturn(response);

        AddressResponse result = manageAddressUseCase.createAddress(userId, request);

        assertThat(result).isEqualTo(response);
        verify(addressRepository).clearDefaultByUserId(userId);
    }

    @Test
    @DisplayName("Deve lancar excecao ao criar endereco quando o usuario nao existir")
    void shouldThrowExceptionWhenCreatingAddressForNonExistentUser() {
        UUID userId = UUID.randomUUID();
        AddressRequest request = new AddressRequest("Rua A", "100", null, "Cidade", "SP", ZIP_CODE, false);
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> manageAddressUseCase.createAddress(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover endereco quando ele pertencer ao usuario informado")
    void shouldDeleteAddressWhenItBelongsToInformedUser() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Address address = Address.builder().id(addressId).userId(userId).build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        manageAddressUseCase.deleteAddress(userId, addressId);

        verify(addressRepository).delete(address);
    }

    @Test
    @DisplayName("Deve lancar excecao ao remover endereco quando ele pertencer a outro usuario")
    void shouldThrowExceptionWhenDeletingAddressThatBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Address address = Address.builder().id(addressId).userId(otherUserId).build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> manageAddressUseCase.deleteAddress(userId, addressId))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).delete(any());
    }

}
