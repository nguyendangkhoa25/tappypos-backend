package com.tappy.pos.model.enums;

/**
 * Lifecycle of a subscription payment.
 * PENDING → PAID (activated) | FAILED | (later) REFUNDED.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
