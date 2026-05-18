package com.tappy.pos.model.enums;

import java.util.Set;

/**
 * Product type codes whose items are unique physical pieces.
 * On sale: product status flips ACTIVE → INACTIVE ("Còn hàng" → "Đã bán").
 * No quantity decrement — inventory stays as a 1-unit availability flag.
 */
public final class UniqueItemProductTypes {

    public static final Set<String> CODES = Set.of("JEWELRY", "WATCH");

    public static boolean isUniqueItem(String productTypeCode) {
        return productTypeCode != null && CODES.contains(productTypeCode);
    }

    private UniqueItemProductTypes() {}
}
