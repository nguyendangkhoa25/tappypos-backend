package com.tappy.pos.model.enums;

import java.util.Set;

/**
 * Product type codes whose items are unique physical pieces.
 * On sale: product status flips ACTIVE → INACTIVE ("Còn hàng" → "Đã bán").
 * No quantity decrement — inventory stays as a 1-unit availability flag.
 */
public final class UniqueItemProductTypes {

    /** Delegates to InventoryMode — single source of truth for UNIQUE type codes. */
    public static final Set<String> CODES = InventoryMode.UNIQUE_CODES;

    public static boolean isUniqueItem(String productTypeCode) {
        return productTypeCode != null && CODES.contains(productTypeCode);
    }

    /** Returns true when a product is UNIQUE by type OR because it came from a pawn contract. */
    public static boolean isUniqueItem(String productTypeCode, Long sourcePawnId) {
        return sourcePawnId != null || isUniqueItem(productTypeCode);
    }

    private UniqueItemProductTypes() {}
}
