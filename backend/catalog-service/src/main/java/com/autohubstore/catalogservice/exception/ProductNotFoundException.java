package com.autohubstore.catalogservice.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String identifier) {
        super("Produto não encontrado: " + identifier);
    }

}
