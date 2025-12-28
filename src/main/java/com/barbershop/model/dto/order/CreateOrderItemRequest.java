package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderItemRequest {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amountBeforeTax;
    private BigDecimal amount;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private Long assignedEmployeeId;
}
