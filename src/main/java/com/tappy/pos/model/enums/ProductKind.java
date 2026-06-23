package com.tappy.pos.model.enums;

/**
 * Distinguishes a sellable finished good from a raw ingredient (Phase 3, two-stage inventory).
 * Default {@link #FINISHED} so every existing product and all non-bakery shop types are unaffected.
 */
public enum ProductKind {
    /** Sellable finished good (default) — appears on POS/sell surfaces. */
    FINISHED,
    /** Raw material consumed by recipes; never sold directly (excluded from POS). */
    INGREDIENT,
    /** Both sold and used as an ingredient (e.g. whipping cream). */
    BOTH
}
