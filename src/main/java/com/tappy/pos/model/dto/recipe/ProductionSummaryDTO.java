package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Production totals over a period (mẻ sản xuất), for the reports tab / dashboard. */
@Data
@Builder
public class ProductionSummaryDTO {
    private long batchCount;          // COMPLETED batches
    private long spoiledCount;        // SPOILED batches
    private BigDecimal totalUnitsProduced;
    private BigDecimal totalIngredientCost;
    private long todayBatchCount;     // COMPLETED batches created today
    private BigDecimal todayUnitsProduced;
}
