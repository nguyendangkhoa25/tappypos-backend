package com.tappy.pos.model.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkItemDTO {

    private Long itemId;
    private Long orderId;
    private String orderNumber;
    private String customerName;

    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Integer durationMinutes;

    private String status;
    private LocalDateTime completedAt;

    private Long assignedEmployeeId;
    private String assignedEmployeeName;

    private LocalDateTime orderCreatedAt;

    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
}
