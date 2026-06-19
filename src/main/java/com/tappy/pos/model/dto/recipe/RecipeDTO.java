package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RecipeDTO {
    private Long id;
    private Long finishedProductId;
    private String finishedProductName;
    private BigDecimal yieldQuantity;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private String notes;
    private List<RecipeItemDTO> items;

    // Derived (live, from current ingredient costs)
    private BigDecimal ingredientCost;   // Σ line costs
    private BigDecimal totalCost;        // ingredientCost + labor + overhead
    private BigDecimal unitCost;         // totalCost / yieldQuantity  (giá vốn 1 cái)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
