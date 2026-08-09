package com.autohubstore.userservice.exception;

public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException(final String message) {
        super(message);
    }

}
