package com.autohubstore.catalogservice.exception;

public class ImageTooLargeException extends RuntimeException {

    public ImageTooLargeException(String fileName) {
        super("Arquivo excede o tamanho máximo permitido: " + fileName);
    }

}
