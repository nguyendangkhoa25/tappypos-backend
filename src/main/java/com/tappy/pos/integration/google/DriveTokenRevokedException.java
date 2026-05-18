package com.tappy.pos.integration.google;

/**
 * Thrown when Google rejects a refresh token with {@code error=invalid_grant}.
 * Callers should mark the integration as DISCONNECTED so the user is prompted to reconnect.
 */
public class DriveTokenRevokedException extends RuntimeException {
    public DriveTokenRevokedException(String message) {
        super(message);
    }
}
