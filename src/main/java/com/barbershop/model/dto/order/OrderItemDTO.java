package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private String status;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private LocalDateTime completedAt;
}
