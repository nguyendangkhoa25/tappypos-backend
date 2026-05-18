package com.tappy.pos.exception;

/**
 * Exception thrown when a business rule or validation fails
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

