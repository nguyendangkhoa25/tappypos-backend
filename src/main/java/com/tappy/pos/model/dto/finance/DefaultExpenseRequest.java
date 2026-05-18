package com.tappy.pos.model.dto.finance;

import com.tappy.pos.model.enums.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DefaultExpenseRequest {
    @NotBlank
    private String description;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private ExpenseCategory category;

    private Integer paymentDay;

    private Integer displayOrder;
}
