package com.tappy.pos.model.enums;

/** Lifecycle of a single customer debt (công nợ) row. */
public enum DebtStatus {
    OPEN,     // nothing paid yet
    PARTIAL,  // partially paid, balance remaining
    PAID      // fully settled
}
