package com.autohubstore.userservice.controller;

import com.autohubstore.userservice.controller.docs.AddressControllerDocs;
import com.autohubstore.userservice.domain.dto.request.AddressRequest;
import com.autohubstore.userservice.domain.dto.response.AddressResponse;
import com.autohubstore.userservice.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController implements AddressControllerDocs {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> findAddresses(@PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.OK).body(addressService.findAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@PathVariable UUID userId,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(userId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID userId,
                                              @PathVariable UUID addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
