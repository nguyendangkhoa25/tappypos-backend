package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCommissionDTO {
    private Long employeeId;
    private String employeeName;
    private int month;
    private int year;
    private BigDecimal totalCommission;
    private long itemCount;
    private List<CommissionItemDTO> items;
}
