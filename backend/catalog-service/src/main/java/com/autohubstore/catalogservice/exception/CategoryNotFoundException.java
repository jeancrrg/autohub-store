package com.autohubstore.catalogservice.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String identifier) {
        super("Categoria não encontrada: " + identifier);
    }

}
