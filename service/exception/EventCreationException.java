package com.palveo.service.exception;

public class EventCreationException extends EventOperationException {
    public EventCreationException(String message) {
        super(message);
    }

    public EventCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}