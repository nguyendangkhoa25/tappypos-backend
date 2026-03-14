package com.knp.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private String status;

    // Customer
    private Long customerId;
    private String customerName;

    // Financials
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal changeAmount;

    private String notes;
    private String createdBy;
    private LocalDateTime completedAt;
    private String completedBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String cancelledBy;
    private LocalDateTime voidedAt;
    private String voidReason;
    private String voidedBy;
    private LocalDateTime createdAt;

    private Long invoiceId;
    private String invoiceNumber;

    private String promotionCode;
    private BigDecimal promotionDiscount;
    private Integer loyaltyPointsRedeemed;
    private BigDecimal loyaltyDiscount;

    private String tableLabel;
    private String source;

    private List<OrderItemDTO> items;
}
