package com.tappy.pos.model.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductionBatchDTO {
    private Long id;
    private Long finishedProductId;
    private String finishedProductName;
    private Long recipeId;
    private BigDecimal quantityProduced;
    private BigDecimal ingredientCost;
    private BigDecimal unitCost;
    private String status;
    private String producedBy;
    private String notes;
    private LocalDateTime createdAt;
}
