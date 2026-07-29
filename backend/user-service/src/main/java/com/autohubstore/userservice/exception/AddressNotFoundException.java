package com.autohubstore.userservice.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String identifier) {
        super("Endereço não encontrado: " + identifier);
    }
}
