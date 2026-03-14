package com.knp.model.enums;

/**
 * Cart Status Enum
 * Represents the status of a shopping cart
 */
public enum CartStatus {
    ACTIVE,        // Currently being used
    ABANDONED,     // User left without completing
    COMPLETED,     // Converted to order
    PAID           // Payment confirmed
}

