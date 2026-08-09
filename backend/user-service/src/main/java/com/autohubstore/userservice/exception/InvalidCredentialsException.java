package com.autohubstore.userservice.exception;

public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(final String message) {
        super(message);
    }

}
