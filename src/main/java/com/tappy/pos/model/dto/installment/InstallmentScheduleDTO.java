package com.tappy.pos.model.dto.installment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InstallmentScheduleDTO {
    private Long id;
    private Integer installmentNo;
    private LocalDate dueDate;
    private BigDecimal amount;
    private boolean paid;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private boolean overdue;     // !paid && dueDate < today
}
