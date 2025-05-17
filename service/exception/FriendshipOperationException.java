package com.palveo.service.exception;

public class FriendshipOperationException extends Exception {
    public FriendshipOperationException(String message) {
        super(message);
    }

    public FriendshipOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
