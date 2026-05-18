package com.tappy.pos.model.dto.finance;

import com.tappy.pos.model.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DefaultExpenseDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String categoryDisplayName;
    private Integer paymentDay;
    private Integer displayOrder;
}
