package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSalaryRequest {
    private BigDecimal deductions;
    private BigDecimal overtime;
    private BigDecimal bonus;
    private String notes;
    private String status;
}

