package com.tappy.pos.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Fashion / variant analytics over a trailing window: best-selling size/color SKUs,
 * dead stock (in stock but not selling), and overall sell-through. Computed from
 * completed-order line items (which carry variant_id) plus per-variant inventory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FashionReportDTO {

    private int windowDays;
    private long totalSold;
    private long totalOnHand;
    /** sold / (sold + on-hand) × 100, rounded to 1 decimal; 0 when there is nothing to sell. */
    private double sellThroughPct;

    private List<VariantStat> bestSellers;
    private List<VariantStat> deadStock;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantStat {
        private Long variantId;
        private Long productId;
        private String productName;
        private String sku;
        private Map<String, String> options;
        private long sold;
        private long onHand;
    }
}
