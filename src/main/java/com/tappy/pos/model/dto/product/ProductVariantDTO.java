package com.tappy.pos.model.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantDTO {

    private Long id;
    private Long productId;
    private String sku;
    private String barcode;
    private Map<String, String> variantOptions;
    private BigDecimal priceOverride;
    /** Resolved price: priceOverride if set, otherwise parent product price. */
    private BigDecimal price;
    private BigDecimal costOverride;
    private String status;
    /** Current stock count from the variant's inventory record; null if no record exists. */
    private Long quantityInStock;
}
