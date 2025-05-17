package com.palveo.service.exception;

public class CommentNotFoundException extends CommentOperationException {
    public CommentNotFoundException(String message) { super(message); }
    public CommentNotFoundException(String message, Throwable cause) { super(message, cause); }
}