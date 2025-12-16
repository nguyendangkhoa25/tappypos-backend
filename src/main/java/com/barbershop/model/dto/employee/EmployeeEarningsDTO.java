package com.barbershop.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeEarningsDTO {
    private Long employeeId;
    private String employeeName;
    private BigDecimal totalEarned;
    private Integer completedOrderCount;
}
