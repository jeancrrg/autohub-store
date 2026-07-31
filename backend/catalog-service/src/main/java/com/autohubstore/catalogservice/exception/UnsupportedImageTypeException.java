package com.autohubstore.catalogservice.exception;

public class UnsupportedImageTypeException extends RuntimeException {

    public UnsupportedImageTypeException(final String contentType) {
        super("Tipo de arquivo não suportado: " + contentType);
    }

}
