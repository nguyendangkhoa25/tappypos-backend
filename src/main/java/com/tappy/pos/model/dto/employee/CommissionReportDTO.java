package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionReportDTO {
    private int month;
    private int year;
    private BigDecimal totalCommission;
    private long totalItemCount;
    private List<EmployeeCommissionDTO> employees;
}
