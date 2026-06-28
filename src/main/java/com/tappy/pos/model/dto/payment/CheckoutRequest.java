package com.tappy.pos.model.dto.payment;

import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import lombok.Data;

/**
 * Request body for POST /payments/checkout. The amount is NOT accepted from the client — it is
 * derived server-side from {@code planCode} + {@code billingCycle} via SubscriptionPlan.LIMITS.
 */
@Data
public class CheckoutRequest {
    private String planCode;            // STARTER | BASIC | PRO | ENTERPRISE | GOLD_PAWN
    private BillingCycle billingCycle;  // MONTHLY | YEARLY
    private PaymentProvider method;     // MOMO | VNPAY | VIETQR
}
