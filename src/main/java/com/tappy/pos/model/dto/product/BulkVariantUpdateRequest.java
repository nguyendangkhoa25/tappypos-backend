package com.tappy.pos.model.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk update of a product's variant matrix (the fashion size × color grid editor).
 * Each cell targets one existing variant by id and may set its price/cost override and/or
 * its absolute stock level in a single round-trip. Null fields are left unchanged, except
 * {@code priceOverride}/{@code costOverride} which are always written (null clears the override).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkVariantUpdateRequest {

    @NotNull
    @NotEmpty
    @Valid
    private List<Cell> variants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cell {
        @NotNull
        private Long variantId;

        private BigDecimal priceOverride;

        private BigDecimal costOverride;

        /** Absolute target stock for this SKU. If null, stock is left unchanged. */
        private Long quantityInStock;
    }
}
