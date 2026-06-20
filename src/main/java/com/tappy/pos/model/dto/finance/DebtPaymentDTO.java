package com.tappy.pos.model.dto.finance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DebtPaymentDTO {
    private Long id;
    private Long customerId;
    private Long debtId;
    private BigDecimal amount;
    private String method;
    private String note;
    private LocalDateTime paidAt;
    private String createdBy;
}
