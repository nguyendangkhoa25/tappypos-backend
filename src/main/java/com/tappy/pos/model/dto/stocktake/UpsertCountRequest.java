package com.tappy.pos.model.dto.stocktake;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Upsert a counted quantity for one product in a session.
 * Identify the product by {@code productId} OR {@code barcode} (productId wins if both set).
 * {@code countedQty} is the absolute real quantity (not a delta).
 */
@Data
public class UpsertCountRequest {
    private Long productId;
    private String barcode;

    @NotNull
    @PositiveOrZero(message = "Counted quantity must be zero or greater")
    private Long countedQty;

    private String note;
}
