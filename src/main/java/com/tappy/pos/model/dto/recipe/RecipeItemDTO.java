package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RecipeItemDTO {
    private Long id;
    private Long ingredientProductId;
    private String ingredientName;
    private String ingredientUnit;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal ingredientUnitCost;  // current cost of one ingredient unit
    private BigDecimal lineCost;            // quantity × ingredientUnitCost
}
