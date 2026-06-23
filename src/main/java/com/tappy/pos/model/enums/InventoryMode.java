package com.tappy.pos.model.enums;

/**
 * Describes how a product's inventory is managed.
 *
 * NO_INVENTORY — intangible services; no stock row, no stock checks.
 * UNIQUE       — single physical item (jewelry, watch, vehicle, pawn-origin).
 *                Auto-created with qty=1; marked INACTIVE ("Đã bán") after sale.
 * TRACKED      — multi-quantity goods; full inventory management required.
 */
public enum InventoryMode {
    NO_INVENTORY,
    UNIQUE,
    TRACKED;

    /** Derives the mode from product type code + whether it came from a pawn contract. */
    public static InventoryMode derive(String typeCode, Long sourcePawnId) {
        if (sourcePawnId != null)       return UNIQUE;
        if ("SERVICE".equals(typeCode)) return NO_INVENTORY;
        if (UNIQUE_CODES.contains(typeCode)) return UNIQUE;
        return TRACKED;
    }

    public static final java.util.Set<String> UNIQUE_CODES =
            java.util.Set.of("JEWELRY", "WATCH", "BIKE", "CAR", "MOTORBIKE",
                             "E_BIKE", "BICYCLE",
                             "SILVER", "SILVER_FIXED", "GEM_DIAMOND");
}
