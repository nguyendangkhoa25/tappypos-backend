package com.tappy.pos.model.dto.inventory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for POST /inventory/adjust.
 * Sent by the mobile app to add or remove stock for a product by its productId.
 * A positive {@code quantity} adds stock; a negative {@code quantity} removes stock.
 */
@Data
public class AdjustInventoryRequest {

    /** The product whose inventory should be adjusted. */
    @NotNull
    private Long productId;

    /**
     * Quantity delta. Positive = add stock, negative = remove stock.
     * Zero is accepted and is a no-op (returns current state).
     */
    @NotNull
    private Long quantity;

    /** Human-readable reason for the adjustment (e.g. "Nhập hàng", "Trả hàng"). */
    private String reason;

    /** Optional free-text note. */
    private String note;
}
