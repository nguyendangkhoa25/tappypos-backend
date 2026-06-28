package com.tappy.pos.model.enums;

/**
 * Payment rails. Web sells via MoMo/VNPay/VietQR; the mobile app must use the store IAP
 * (Apple/Google) for digital subscriptions. See docs/SUBSCRIPTION_PAYMENTS.md.
 */
public enum PaymentProvider {
    MOMO,
    VNPAY,
    VIETQR,
    APPLE_IAP,
    GOOGLE_IAP
}
