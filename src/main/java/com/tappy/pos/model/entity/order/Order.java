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

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Builder.Default
    @Column(name = "tip_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "discount_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** FnB service charge (phí dịch vụ): percentage applied to the discounted subtotal, added as its own line. */
    @Builder.Default
    @Column(name = "service_charge_rate", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "service_charge_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(length = 500)
    private String notes;

    // i18n for system-generated notes/reasons (split/merge/reject). When set, these take precedence
    // over the literal columns above and render in the reader's locale (OrderServiceImpl.mapToDTO).
    // User-authored notes/reasons keep using the literal columns. See V038__order_notes_i18n.sql.

    @Column(name = "notes_key", length = 150)
    private String notesKey;

    @Column(name = "notes_args", columnDefinition = "text")
    private String notesArgs;

    /** Pharmacy dispensing record (PHARMACY §4d): who prescribed + a free-text note,
     *  captured at checkout when the cart contains a prescription-required drug. Nullable. */
    @Column(name = "prescriber_name", length = 255)
    private String prescriberName;

    @Column(name = "prescription_note", length = 1000)
    private String prescriptionNote;

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

    @Column(name = "cancel_reason_key", length = 150)
    private String cancelReasonKey;

    @Column(name = "cancel_reason_args", columnDefinition = "text")
    private String cancelReasonArgs;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "void_reason_key", length = 150)
    private String voidReasonKey;

    @Column(name = "void_reason_args", columnDefinition = "text")
    private String voidReasonArgs;

    @Column(name = "voided_by", length = 100)
    private String voidedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "table_label", length = 100)
    private String tableLabel;

    // i18n for a system-generated table label (e.g. lodging "Phòng {0}"). When set, takes precedence
    // over the literal column and renders in the reader's locale. Real F&B table / booking resource
    // names stay in the literal column. See V039__order_table_label_i18n.sql.
    @Column(name = "table_label_key", length = 150)
    private String tableLabelKey;

    @Column(name = "table_label_args", columnDefinition = "text")
    private String tableLabelArgs;

    // Set for QR customer orders so the owner's confirm step can occupy the right table.
    @Column(name = "table_id")
    private Long tableId;

    /**
     * For a split child check (tách bill): the id of the source order it was split from.
     * NULL for every normal order and for the source order itself. A split group is the
     * source order plus all orders whose {@code parentOrderId} equals the source's id —
     * the table is released only once every member of the group is settled.
     */
    @Column(name = "parent_order_id")
    private Long parentOrderId;

    /**
     * Target pickup time for takeaway orders (null for dine-in / non-F&B orders).
     * Set by staff at order creation; displayed on kitchen tickets and the takeaway queue.
     */
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Builder.Default
    @Column(name = "source", length = 20, nullable = false)
    private String source = "POS";

    /**
     * Marks this order as a pre-order (đơn đặt trước) — created now, paid in full later
     * at pickup. A pre-order is a PENDING order with a future {@link #pickupTime}.
     * Default false so all existing/normal orders are unaffected.
     */
    @Builder.Default
    @Column(name = "is_preorder", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean preorder = false;

    /** Quotation (báo giá): holds items + totals but defers all stock deduction until converted. */
    @Builder.Default
    @Column(name = "is_quote", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean quote = false;

    @Column(name = "quote_number", length = 50)
    private String quoteNumber;

    /**
     * Deposit (tiền cọc) taken at pre-order creation, kept as a distinct figure for the
     * printed phiếu and reports. The running paid total lives in {@link #amountPaid};
     * balance due is derived as {@code totalAmount - amountPaid} and never stored.
     */
    @Builder.Default
    @Column(name = "deposit_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /** Set when this order settles a lodging room stay (ROOM feature); null otherwise. */
    @Column(name = "room_stay_id")
    private Long roomStayId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'SELL'")
    private OrderType orderType = OrderType.SELL;

    /** FnB fulfilment channel (dine-in / takeaway / delivery). Distinct from {@link OrderType} (gold). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_channel", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'DINE_IN'")
    private OrderChannel orderChannel = OrderChannel.DINE_IN;

    // ── Delivery details — populated only when orderChannel = DELIVERY ───────────

    /** Delivery platform: GRAB_FOOD / SHOPEE_FOOD / BE_FOOD / SELF (shop's own shipper). */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_platform", length = 30)
    private DeliveryPlatform deliveryPlatform;

    @Column(name = "delivery_recipient", length = 150)
    private String deliveryRecipient;

    @Column(name = "delivery_phone", length = 50)
    private String deliveryPhone;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Builder.Default
    @Column(name = "delivery_fee", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private DeliveryStatus deliveryStatus;

    /** Exactly-once guard for loyalty stamp-card accrual; set after stamps are awarded. */
    @Builder.Default
    @Column(name = "stamps_awarded", nullable = false)
    private boolean stampsAwarded = false;

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
    @Column(name = "promotion_discount", precision = 15, scale = 2)
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "loyalty_points_redeemed")
    private Integer loyaltyPointsRedeemed = 0;

    @Builder.Default
    @Column(name = "loyalty_discount", precision = 15, scale = 2)
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

    public enum OrderChannel {
        DINE_IN,
        TAKEAWAY,
        DELIVERY
    }

    public enum DeliveryPlatform {
        GRAB_FOOD,
        SHOPEE_FOOD,
        BE_FOOD,
        SELF
    }

    public enum DeliveryStatus {
        PENDING,     // awaiting pickup by driver
        DELIVERING,  // out for delivery
        DELIVERED,   // handed to customer
        CANCELLED
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

    /** Cancel with a system-generated, i18n reason (rendered in the reader's locale at read time). */
    public void cancel(String reasonKey, String reasonArgsJson, String cancelledBy) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReasonKey = reasonKey;
        this.cancelReasonArgs = reasonArgsJson;
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

    /** Void with a system-generated, i18n reason (rendered in the reader's locale at read time). */
    public void voidOrder(String reasonKey, String reasonArgsJson, String voidedBy) {
        this.status = OrderStatus.VOIDED;
        this.voidedAt = LocalDateTime.now();
        this.voidReasonKey = reasonKey;
        this.voidReasonArgs = reasonArgsJson;
        this.voidedBy = voidedBy;
    }
}

