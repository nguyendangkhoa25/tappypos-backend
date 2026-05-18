package com.tappy.pos.model.dto.order;

import com.tappy.pos.model.entity.order.OrderItem;
import lombok.*;

import java.math.BigDecimal;

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
    private BigDecimal amount;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private OrderItem.ItemType itemType;
    private String metadata;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
}
