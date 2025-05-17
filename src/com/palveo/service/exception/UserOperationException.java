package com.palveo.service.exception;

public class UserOperationException extends Exception {
    public UserOperationException(String message) {
        super(message);
    }

    public UserOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
