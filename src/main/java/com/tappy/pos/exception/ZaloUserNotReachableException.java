package com.tappy.pos.exception;

/**
 * Thrown when Zalo ZNS reports that the recipient cannot receive the message —
 * the phone number has not followed / interacted with the Tappy Official Account
 * (Zalo error codes -124, -118, -134).
 *
 * <p>Surfaced to the user as a recovery prompt (HTTP 422 {@code USER_NOT_ON_ZALO})
 * so they learn <em>why</em> no OTP arrived and how to get help, instead of waiting
 * forever for a code that can never be delivered.
 */
public class ZaloUserNotReachableException extends RuntimeException {
    public ZaloUserNotReachableException(String message) {
        super(message);
    }
}
