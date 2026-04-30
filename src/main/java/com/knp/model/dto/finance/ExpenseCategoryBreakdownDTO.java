package com.knp.model.dto.finance;

import com.knp.model.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExpenseCategoryBreakdownDTO {
    private ExpenseCategory category;
    private String categoryDisplayName;
    private BigDecimal total;
    private double percentage;
}
