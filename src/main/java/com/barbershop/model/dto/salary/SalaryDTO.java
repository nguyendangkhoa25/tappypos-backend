package com.barbershop.model.dto.salary;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer month;
    private Integer year;
    private BigDecimal netSalary;
    private BigDecimal commissionAmount;
    private BigDecimal deductionAmount;
    private BigDecimal overtimeAmount;
    private BigDecimal bonusAmount;
    private BigDecimal allowanceAmount;
    private String notes;
    private String status;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

