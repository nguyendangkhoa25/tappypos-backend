package com.tappy.pos.model.dto.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Record a credit sale / manual debt (ghi nợ). orderId/orderNumber set when created from an order. */
@Data
public class CreateDebtRequest {
    @NotNull
    private Long customerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private Long orderId;
    private String orderNumber;
    private LocalDate dueDate;
    private String note;
}
