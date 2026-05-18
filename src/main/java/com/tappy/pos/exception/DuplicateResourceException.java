package com.tappy.pos.exception;

/**
 * Exception thrown when attempting to create a resource that already exists
 * (e.g., duplicate username, email, etc.)
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}

