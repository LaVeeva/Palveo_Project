package com.palveo.service.exception;

public class TagOperationException extends Exception {
    public TagOperationException(String message) {
        super(message);
    }

    public TagOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
