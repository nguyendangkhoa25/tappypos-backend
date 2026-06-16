package com.tappy.pos.model.dto.stocktake;

import lombok.Builder;
import lombok.Data;

/**
 * A product resolved for counting — returned by barcode lookup and the "uncounted" list.
 * {@code alreadyCountedQty} is null when the product has not been counted in the session yet.
 */
@Data
@Builder
public class StocktakeProductLineDTO {
    private Long productId;
    private String productName;
    private String sku;
    private String barcode;
    private Long expectedQty;
    private Long alreadyCountedQty;
}
