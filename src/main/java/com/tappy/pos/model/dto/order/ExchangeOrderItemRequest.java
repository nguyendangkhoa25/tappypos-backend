package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Size/color swap on a completed order line: return the item's current variant and
 * issue {@code newVariantId} of the same product instead. Only equal-price swaps are
 * allowed, so the order total and payment are left unchanged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeOrderItemRequest {

    @NotNull
    private Long newVariantId;
}
