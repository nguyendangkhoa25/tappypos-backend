package com.tappy.pos.model.enums;

/**
 * Lifecycle of a stocktake (physical inventory count) session.
 * IN_PROGRESS → counts can be added/edited.
 * COMPLETED   → counts applied to inventory; read-only.
 * CANCELLED   → abandoned; no stock change.
 */
public enum StocktakeStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
