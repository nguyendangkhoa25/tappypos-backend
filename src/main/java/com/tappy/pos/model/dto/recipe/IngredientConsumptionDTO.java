package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** How much of one ingredient was consumed by production over a period (tiêu hao nguyên liệu). */
@Data
@Builder
public class IngredientConsumptionDTO {
    private Long ingredientProductId;
    private String ingredientName;
    private String unit;
    private BigDecimal totalQuantity;
    private BigDecimal totalCost;
}
