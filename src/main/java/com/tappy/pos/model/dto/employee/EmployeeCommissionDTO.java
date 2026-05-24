package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCommissionDTO {
    private Long employeeId;
    private String employeeName;
    private BigDecimal totalCommission;
    private long itemCount;
}
