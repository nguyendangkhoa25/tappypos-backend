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
    private BigDecimal deductions;
    private BigDecimal overtime;
    private BigDecimal bonus;
    private String notes;
}

