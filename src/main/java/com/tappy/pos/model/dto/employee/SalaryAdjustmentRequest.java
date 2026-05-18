package com.tappy.pos.model.dto.employee;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SalaryAdjustmentRequest {
    private String type; // BONUS | DEDUCTION
    private BigDecimal amount;
    private String note;
}
