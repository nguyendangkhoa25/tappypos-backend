package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryAdvanceDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private BigDecimal amount;
    private LocalDate advanceDate;
    private String note;
    private Long salaryId;
    private boolean deducted;
    private String createdBy;
    private LocalDateTime createdAt;
}
