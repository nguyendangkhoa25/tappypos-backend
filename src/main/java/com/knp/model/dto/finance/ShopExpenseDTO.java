package com.knp.model.dto.finance;

import com.knp.model.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ShopExpenseDTO {
    private Long id;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String categoryDisplayName;
    private String description;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String referenceNumber;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
