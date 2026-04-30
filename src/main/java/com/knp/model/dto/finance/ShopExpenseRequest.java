package com.knp.model.dto.finance;

import com.knp.model.enums.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ShopExpenseRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private ExpenseCategory category;

    private String description;

    @NotNull
    private LocalDate expenseDate;

    private String paymentMethod;
    private String referenceNumber;
}
