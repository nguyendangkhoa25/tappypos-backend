package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSalaryRequest {
    private BigDecimal netSalary;
    private BigDecimal commissionAmount;
    private BigDecimal deductionAmount;
    private BigDecimal overtimeAmount;
    private BigDecimal bonusAmount;
    private BigDecimal allowanceAmount;
    private String notes;
    private String status;
}

