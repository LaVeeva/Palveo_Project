package com.palveo.service.exception;

public class RatingOperationException extends Exception {
    public RatingOperationException(String message) {
        super(message);
    }

    public RatingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
