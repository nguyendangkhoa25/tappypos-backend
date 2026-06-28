package com.tappy.pos.service.payment;

import com.tappy.pos.model.dto.payment.CheckoutRequest;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;

import java.util.List;
import java.util.Map;

public interface SubscriptionPaymentService {

    /** Start a renewal payment for the current tenant. Amount is derived server-side from the plan. */
    CheckoutResponse createCheckout(CheckoutRequest request);

    /** Handle a verified provider callback (idempotent). Activates the subscription on success. */
    void handleCallback(PaymentProvider provider, Map<String, String> params);

    /** Master-admin manual confirmation of a (VietQR) bank transfer → activate. */
    void confirmManual(String txnRef);

    /** Master-admin refund of a PAID payment → marks it REFUNDED (idempotent). Does not change expiry. */
    void refund(String txnRef);

    /**
     * Master-admin operator-recorded offline payment (cash / direct transfer): creates a PAID
     * {@link PaymentProvider#MANUAL} payment and extends the tenant's subscription. Amount is derived
     * server-side from the plan.
     */
    void recordOfflinePayment(String tenantId, String planCode, BillingCycle billingCycle, String note);

    /** Recent payments for the current tenant (newest first). */
    List<Map<String, Object>> historyForCurrentTenant();
}
