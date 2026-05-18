package com.tappy.pos.model.enums;

import java.util.Set;

/**
 * Product type codes that have no physical inventory (e.g. services).
 * Cart add-to-cart and checkout skip all inventory checks for these types.
 */
public final class NoInventoryProductTypes {

    public static final Set<String> CODES = Set.of("SERVICE");

    public static boolean isNoInventory(String productTypeCode) {
        return productTypeCode != null && CODES.contains(productTypeCode);
    }

    private NoInventoryProductTypes() {}
}
