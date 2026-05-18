package com.tappy.pos.model.enums;

import java.util.Set;

/**
 * Product type codes whose sell price is computed at order time from the
 * current market rate rather than stored at product creation.
 *
 * When a product of one of these types is added to the cart, CartServiceImpl
 * fetches the current gold price for the product's category and computes:
 *   unitPrice = gold_weight × current_sell_price + proc_fee
 *
 * The product's stored price field is ignored (it is always 0).
 */
public final class DynamicPriceProductTypes {

    public static final Set<String> CODES = Set.of("JEWELRY");

    public static boolean isDynamicPrice(String productTypeCode) {
        return productTypeCode != null && CODES.contains(productTypeCode);
    }

    private DynamicPriceProductTypes() {}
}
