package com.tappy.pos.model.dto.recipe;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Run a production batch (làm bánh): make {@code quantity} units of the finished product. */
@Data
@NoArgsConstructor
public class ProduceRequest {

    @NotNull(message = "finishedProductId is required")
    private Long finishedProductId;

    @NotNull(message = "quantity is required")
    private BigDecimal quantity;

    private String notes;

    /** When true, set the finished product's cost_price to the produced unit cost. Default false (suggest-only). */
    private boolean updateCostPrice;
}
