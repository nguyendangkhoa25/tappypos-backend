package com.tappy.pos.service.payment;

import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.enums.PaymentProvider;

import java.util.Map;

/**
 * A payment rail (MoMo, VNPay, VietQR, …). Implementations build the provider-specific checkout and
 * verify the provider's server-to-server callback. The service layer owns persistence + activation.
 */
public interface PaymentGateway {

    PaymentProvider provider();

    /**
     * Build the checkout for a freshly-created PENDING payment.
     * @throws com.tappy.pos.exception.BadRequestException if the provider is not configured.
     */
    CheckoutResult createCheckout(SubscriptionPayment payment);

    /**
     * Verify and parse a provider callback. Returns the matched txn reference, whether the provider
     * signature verified, and whether the payment succeeded. Implementations MUST verify the
     * signature; {@code success} can only be true when {@code signatureValid} is true. The service
     * acts on (records) a callback only when {@code signatureValid} is true, so an unsigned/forged
     * callback can never mutate a payment row.
     */
    CallbackResult handleCallback(Map<String, String> params);

    /** Provider-specific checkout details; the service fills the common fields (txnRef, amount, …). */
    record CheckoutResult(
            CheckoutResponse.Type type,
            String payUrl,        // REDIRECT
            String qrContent,     // QR
            String bankAccount,
            String bankName,
            String accountName,
            String transferNote) {}

    /**
     * Verified outcome of a callback. {@code signatureValid} gates whether the service acts on it at
     * all; {@code success} then distinguishes a confirmed payment (PAID) from a validly-signed but
     * declined one (FAILED). {@code success} is always false when {@code signatureValid} is false.
     */
    record CallbackResult(String txnRef, boolean signatureValid, boolean success) {}
}
