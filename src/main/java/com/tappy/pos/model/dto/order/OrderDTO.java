package com.tappy.pos.model.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private BigDecimal tipAmount;
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
    private String orderType;

    private List<OrderItemDTO> items;

    // ── Alias getters for mobile client backward compatibility ─────────────────
    // Jackson serialises both the field getter (totalAmount) and these,
    // so the response includes both names without any breaking change.

    public BigDecimal getTotal() { return totalAmount; }

    public BigDecimal getDiscount() { return discountAmount; }

    public BigDecimal getSubtotal() {
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal tip  = tipAmount     != null ? tipAmount     : BigDecimal.ZERO;
        BigDecimal tot  = totalAmount   != null ? totalAmount   : BigDecimal.ZERO;
        return tot.add(disc).subtract(tip);
    }

    public String getNote() { return notes; }

    public String getCreatedByName() { return createdBy; }
}
