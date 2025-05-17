package com.palveo.service.exception;

public class TagNotFoundException extends TagOperationException {
    public TagNotFoundException(String message) {
        super(message);
    }

    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
