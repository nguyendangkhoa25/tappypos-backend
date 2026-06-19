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
    private Long variantId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private OrderItem.ItemType itemType;
    private String metadata;
    /** Chosen modifiers as JSON: [{groupName, optionName, priceDelta}] (FnB). */
    private String modifiers;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;

    /** Per-item customer note (e.g. "ít đường", "không hành"). */
    private String note;

    /** Kitchen status: "PENDING" | "IN_PROGRESS" | "COMPLETED" */
    private String itemStatus;

    /** Service duration in minutes (0 = no timer). Snapshotted from product at order time. */
    private Integer durationMinutes;

    // Alias for mobile client
    public BigDecimal getSubtotal() { return amount; }
}
