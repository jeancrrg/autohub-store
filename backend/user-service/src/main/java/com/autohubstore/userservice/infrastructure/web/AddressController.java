package com.autohubstore.userservice.infrastructure.web;

import com.autohubstore.userservice.infrastructure.web.docs.AddressControllerDocs;
import com.autohubstore.userservice.application.dto.request.AddressRequest;
import com.autohubstore.userservice.application.dto.response.AddressResponse;
import com.autohubstore.userservice.application.usecase.ManageAddressUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController implements AddressControllerDocs {

    private final ManageAddressUseCase manageAddressUseCase;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> findAddresses(@PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.OK).body(manageAddressUseCase.findAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@PathVariable UUID userId,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageAddressUseCase.createAddress(userId, request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID userId,
                                                          @PathVariable UUID addressId,
                                                          @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(manageAddressUseCase.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID userId,
                                              @PathVariable UUID addressId) {
        manageAddressUseCase.deleteAddress(userId, addressId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
