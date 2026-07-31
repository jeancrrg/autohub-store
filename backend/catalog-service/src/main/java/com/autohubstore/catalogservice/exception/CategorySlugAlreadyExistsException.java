package com.autohubstore.catalogservice.exception;

public class CategorySlugAlreadyExistsException extends RuntimeException {

    public CategorySlugAlreadyExistsException(String slug) {
        super("Slug de categoria já cadastrado: " + slug);
    }

}
