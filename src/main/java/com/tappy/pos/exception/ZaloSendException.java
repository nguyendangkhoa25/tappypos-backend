package com.tappy.pos.exception;

/**
 * Thrown when sending a Zalo ZNS message fails for a transient or technical reason —
 * a network error, ZNS not configured, or a non-zero Zalo error other than
 * "user not reachable".
 *
 * <p>Surfaced as HTTP 502 {@code OTP_SEND_FAILED} so the user can simply retry.
 */
public class ZaloSendException extends RuntimeException {
    public ZaloSendException(String message) {
        super(message);
    }

    public ZaloSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
