package com.tappy.pos.model.dto.finance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row per customer who owes money — the main "Công nợ" list. */
@Data
@Builder
public class CustomerDebtSummaryDTO {
    private Long customerId;
    private String customerName;
    private BigDecimal totalOutstanding;
    private int debtCount;
    private LocalDate earliestDueDate;
    private boolean overdue;
}
