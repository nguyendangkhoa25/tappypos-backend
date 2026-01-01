package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSalaryRequest {
    private Long employeeId;
    private Integer month;
    private Integer year;
    private BigDecimal totalEarning;
    private BigDecimal commissionAmount;
    private BigDecimal deductionAmount;
    private BigDecimal overtimeAmount;
    private BigDecimal bonusAmount;
    private BigDecimal allowanceAmount;
    private String notes;
}

