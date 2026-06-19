package com.tappy.pos.model.dto.order;

import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request body for POST /carts/{cartId}/checkout
 */
@Data
@NoArgsConstructor
public class CheckoutRequest {

    /** Order-level discount amount (on top of any per-item discounts). */
    @DecimalMin(value = "0.0", message = "Discount must be >= 0")
    private BigDecimal discountAmount;

    /** AMOUNT or PERCENTAGE. Defaults to AMOUNT when null. */
    private DiscountType discountType;

    /** Payment method: CASH, CARD, QR. Defaults to CASH. */
    private String paymentMethod;

    /** Cash tendered by the customer (used for change calculation). */
    @DecimalMin(value = "0.0", message = "Amount paid must be >= 0")
    private BigDecimal amountPaid;

    /** Gratuity added on top of the order total (service shops only). */
    @DecimalMin(value = "0.0", message = "Tip must be >= 0")
    private BigDecimal tip;

    /**
     * FnB service-charge rate as a percentage (e.g. 5 = 5%). Null = use the shop-config default;
     * 0 disables it for this order. Applied to the discounted subtotal.
     */
    @DecimalMin(value = "0.0", message = "Service charge must be >= 0")
    private BigDecimal serviceChargeRate;

    /** Fulfilment channel; if null it is derived from tableId (DINE_IN) / pickupTime (TAKEAWAY). */
    private Order.OrderChannel orderChannel;

    private String notes;

    /** ID of an existing customer to associate with this order. */
    private Long customerId;

    /**
     * Free-text guest name when no customer record is needed.
     * Ignored if customerId is supplied.
     */
    private String customerName;

    /** Optional promotion/coupon code to apply. */
    private String promotionCode;

    /** Loyalty points to redeem for a discount (requires a customer). */
    private Integer loyaltyPointsToRedeem;

    /**
     * ID of a PENDING order (kitchen ticket) created via send-to-kitchen.
     * If provided, this order will be cancelled before creating the final COMPLETED order.
     */
    private Long pendingOrderId;

    /**
     * Order type: SELL (default), BUY (shop buys from customer), EXCHANGE (net gold swap).
     * Null defaults to SELL to preserve backward compatibility.
     */
    private Order.OrderType orderType;

    /**
     * When true, creates an IN_PROGRESS order (service started, payment deferred).
     * Used by service shops for the two-phase flow: start service → pay later.
     */
    private boolean createAsInProgress;

    /**
     * Target pickup time for F&B takeaway orders. Null for dine-in orders.
     * When set, the order is treated as takeaway on the kitchen display.
     */
    private LocalDateTime pickupTime;

    /**
     * Surplus/deficit weight in chỉ after an EXCHANGE order
     * (computed client-side from COMPENSATE/RESIDUAL items).
     * Signed: positive = surplus (shop returns gold), negative = deficit (customer owes).
     * Stored on the order for receipt display; does not affect total calculation.
     */
    private BigDecimal goldDiffWeight;

    /**
     * Monetary value (₫) of the surplus/deficit weight.
     * Client supplies this so the receipt can show the "Vàng dư/bù" line.
     */
    private BigDecimal goldDiffAmount;

    // ── Pre-order / deposit (đặt hàng + tiền cọc) — Phase 2 ─────────────────────
    /**
     * When true the checkout creates a PENDING pre-order instead of completing a sale:
     * no inventory is deducted (deferred to settle/pickup), {@code amountPaid} is set to
     * {@link #depositAmount}, and the balance is collected later via the settle endpoint.
     * Defaults to false → identical behaviour to a normal checkout.
     */
    private boolean preorder;

    /**
     * Deposit (tiền cọc) collected at pre-order creation. Must be {@code <= total}.
     * Zero is allowed (đơn đặt không cọc). Ignored when {@link #preorder} is false.
     */
    @DecimalMin(value = "0.0", message = "Deposit must be >= 0")
    private BigDecimal depositAmount;
}
