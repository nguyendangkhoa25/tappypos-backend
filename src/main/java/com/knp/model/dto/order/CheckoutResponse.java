package com.knp.model.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for a completed checkout / order.
 * Sent back to the frontend for the receipt print view.
 */
@Data
@Builder
public class CheckoutResponse {

    private Long orderId;
    private String orderNumber;

    // Shop info (for receipt header)
    private String shopName;
    private String shopAddress;
    private String shopTaxId;

    // Line items
    private List<CheckoutItemSummary> items;

    // Totals
    private BigDecimal subtotal;
    private BigDecimal itemDiscount;
    private BigDecimal orderDiscount;
    private BigDecimal promotionDiscount;
    private BigDecimal loyaltyDiscount;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal total;

    // Promotion / Loyalty
    private String promotionCode;
    private Integer loyaltyPointsRedeemed;

    // Payment
    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal changeAmount;

    // Customer
    private Long customerId;
    private String customerName;

    // Meta
    private String notes;
    private LocalDateTime completedAt;

    @Data
    @Builder
    public static class CheckoutItemSummary {
        private String productName;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountValue;
        private BigDecimal lineTotal;
    }
}
