package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryAdjustmentDTO {
    private Long id;
    private String type; // BONUS | DEDUCTION
    private BigDecimal amount;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
