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
    private BigDecimal totalEarnings;
    private BigDecimal deductions;
    private BigDecimal overtime;
    private BigDecimal bonus;
    private BigDecimal netSalary;
    private String notes;
    private String status;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

