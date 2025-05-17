package com.palveo.service.exception;

public class EventNotFoundException extends EventOperationException {
    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}