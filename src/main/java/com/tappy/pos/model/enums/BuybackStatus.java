package com.tappy.pos.model.enums;

/** Lifecycle of a buyback (second-hand outright purchase). See PAWN_BUYBACK_SPEC §5. */
public enum BuybackStatus {
    PURCHASED,  // bought outright, not yet listed for resale
    LISTED,     // resale product created
    SOLD,       // resale order completed
    CANCELLED   // voided before sale
}
