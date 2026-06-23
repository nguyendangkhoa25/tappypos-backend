package com.tappy.pos.model.dto.buyback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Intake of a used item bought outright (PAWN_BUYBACK_SPEC §6). */
@Data
public class CreateBuybackRequest {

    /** Seller; null = walk-in. CCCD is read from the customer record. */
    private Long customerId;
    private String customerName;

    @NotBlank(message = "{error.buyback.itemNameRequired}")
    private String itemName;

    private String itemDescription;
    private String itemCategory;

    @NotNull(message = "{error.buyback.acquisitionPriceRequired}")
    @PositiveOrZero(message = "{error.buyback.acquisitionPriceRequired}")
    private BigDecimal acquisitionPrice;

    /** Optional; defaults to now. */
    private LocalDateTime purchaseDate;
}
