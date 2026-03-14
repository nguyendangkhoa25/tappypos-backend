package com.knp.model.dto;

import com.knp.model.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
