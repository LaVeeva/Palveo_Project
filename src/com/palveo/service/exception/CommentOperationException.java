package com.palveo.service.exception;

public class CommentOperationException extends Exception {
    public CommentOperationException(String message) { super(message); }
    public CommentOperationException(String message, Throwable cause) { super(message, cause); }
}