package com.tappy.pos.model.dto.buyback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/** Marks a buyback SOLD with its final resale price (PAWN_BUYBACK_SPEC §6). */
@Data
public class SellBuybackRequest {

    @NotNull(message = "{error.buyback.resalePriceRequired}")
    @PositiveOrZero(message = "{error.buyback.resalePriceRequired}")
    private BigDecimal resalePrice;

    /** Optional link to the resale order. */
    private Long orderId;
}
