package com.tappy.pos.model.dto.employee;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryCommissionItemDTO {
    private Long orderItemId;
    private String orderNumber;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private LocalDateTime completedAt;
}
