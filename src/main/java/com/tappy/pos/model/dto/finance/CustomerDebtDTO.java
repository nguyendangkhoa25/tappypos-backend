package com.tappy.pos.model.dto.finance;

import com.tappy.pos.model.enums.DebtStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerDebtDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private String orderNumber;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private DebtStatus status;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
