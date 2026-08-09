package com.autohubstore.userservice.exception;

public class InactiveAccountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InactiveAccountException(final String message) {
        super(message);
    }

}
