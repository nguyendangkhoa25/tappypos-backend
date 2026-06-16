package com.tappy.pos.model.entity.order;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.customer.Customer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @Column(name = "order_number", unique = true, length = 20)
    private String orderNumber;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "change_amount", precision = 10, scale = 2)
    private BigDecimal changeAmount;

    @Builder.Default
    @Column(name = "tip_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "discount_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "voided_by", length = 100)
    private String voidedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "table_label", length = 100)
    private String tableLabel;

    // Set for QR customer orders so the owner's confirm step can occupy the right table.
    @Column(name = "table_id")
    private Long tableId;

    /**
     * Target pickup time for takeaway orders (null for dine-in / non-F&B orders).
     * Set by staff at order creation; displayed on kitchen tickets and the takeaway queue.
     */
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Builder.Default
    @Column(name = "source", length = 20, nullable = false)
    private String source = "POS";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'SELL'")
    private OrderType orderType = OrderType.SELL;

    /** Total value of GOLD_OUT + STANDARD items (what shop sells to customer). */
    @Builder.Default
    @Column(name = "sell_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal sellAmount = BigDecimal.ZERO;

    /** Total value of GOLD_IN items (what shop buys from customer). */
    @Builder.Default
    @Column(name = "buy_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal buyAmount = BigDecimal.ZERO;

    /** Surplus/deficit weight in chỉ after exchange (positive = shop returns gold to customer). */
    @Builder.Default
    @Column(name = "gold_diff_weight", precision = 10, scale = 3, columnDefinition = "DECIMAL(10,3) DEFAULT 0")
    private BigDecimal goldDiffWeight = BigDecimal.ZERO;

    /** Monetary value of the surplus/deficit weight. */
    @Builder.Default
    @Column(name = "gold_diff_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal goldDiffAmount = BigDecimal.ZERO;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Builder.Default
    @Column(name = "promotion_discount", precision = 10, scale = 2)
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "loyalty_points_redeemed")
    private Integer loyaltyPointsRedeemed = 0;

    @Builder.Default
    @Column(name = "loyalty_discount", precision = 10, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public enum OrderStatus {
        SUBMITTED,   // Customer self-submitted via QR; awaiting owner confirmation (not yet in kitchen)
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        VOIDED
    }

    public enum OrderType {
        SELL,
        BUY,
        EXCHANGE
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void complete(String completedBy) {
        complete();
        this.completedBy = completedBy;
    }

    public void cancel(String reason, String cancelledBy) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        this.cancelledBy = cancelledBy;
    }

    public void start() {
        this.status = OrderStatus.IN_PROGRESS;
    }

    /** Owner confirms a customer-submitted (SUBMITTED) order, moving it into the kitchen queue (PENDING). */
    public void confirm(String confirmedBy) {
        this.status = OrderStatus.PENDING;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = confirmedBy;
    }

    public void voidOrder(String reason, String voidedBy) {
        this.status = OrderStatus.VOIDED;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
        this.voidedBy = voidedBy;
    }
}

