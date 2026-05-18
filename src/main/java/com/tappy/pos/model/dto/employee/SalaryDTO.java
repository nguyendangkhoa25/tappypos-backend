package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private BigDecimal baseWage;
    private BigDecimal totalCommission;
    private BigDecimal advanceAmount;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<SalaryCommissionItemDTO> commissionItems;
    private List<SalaryAdjustmentDTO> adjustments;
    private List<SalaryAdvanceDTO> advances;
}
