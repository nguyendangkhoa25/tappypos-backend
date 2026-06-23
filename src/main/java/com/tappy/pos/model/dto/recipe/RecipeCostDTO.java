package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Live cost breakdown + re-price signal for a recipe (giá vốn thật + giá bán đề xuất). */
@Data
@Builder
public class RecipeCostDTO {
    private Long recipeId;
    private Long finishedProductId;
    private BigDecimal ingredientCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal totalCost;
    private BigDecimal yieldQuantity;
    private BigDecimal unitCost;          // giá vốn 1 cái
    private BigDecimal currentSellPrice;  // product.price now
    private BigDecimal suggestedPrice;    // unitCost / (1 - margin)
    private BigDecimal grossMarginPct;    // (sell - unitCost) / sell × 100
    private boolean missingIngredientCost; // some ingredient has no cost_price
    private BigDecimal storedCostPrice;   // product.cost_price now
    private boolean needsReprice;         // recomputed unitCost > stored cost_price → re-price signal
}
