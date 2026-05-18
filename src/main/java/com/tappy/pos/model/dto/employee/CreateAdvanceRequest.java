package com.tappy.pos.model.dto.employee;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreateAdvanceRequest {
    private Long employeeId;
    private BigDecimal amount;
    private LocalDate advanceDate;
    private String note;
}
