package com.tappy.pos.model.dto.recipe;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Create or replace the recipe (định lượng) for a finished product. Items fully replace existing ones. */
@Data
@NoArgsConstructor
public class SaveRecipeRequest {

    @NotNull(message = "finishedProductId is required")
    private Long finishedProductId;

    private BigDecimal yieldQuantity;   // default 1 when null
    private BigDecimal laborCost;       // default 0
    private BigDecimal overheadCost;    // default 0
    private String notes;

    private List<Item> items;

    @Data
    @NoArgsConstructor
    public static class Item {
        @NotNull private Long ingredientProductId;
        @NotNull private BigDecimal quantity;
        private String unit;
    }
}
