package com.palveo.service.exception;

public class EventOperationException extends Exception {
    public EventOperationException(String message) {
        super(message);
    }

    public EventOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}