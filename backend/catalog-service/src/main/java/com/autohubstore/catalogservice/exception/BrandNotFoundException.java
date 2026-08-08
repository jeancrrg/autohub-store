package com.autohubstore.catalogservice.exception;

public class BrandNotFoundException extends RuntimeException {

    public BrandNotFoundException(String identifier) {
        super("Marca não encontrada: " + identifier);
    }

}
