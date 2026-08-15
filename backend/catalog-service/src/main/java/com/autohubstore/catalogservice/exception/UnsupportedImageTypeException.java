package com.autohubstore.catalogservice.exception;

public class UnsupportedImageTypeException extends RuntimeException {

    public UnsupportedImageTypeException(String contentType) {
        super("Tipo de arquivo não suportado: " + contentType);
    }

}
