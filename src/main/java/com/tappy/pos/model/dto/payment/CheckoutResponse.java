package com.tappy.pos.model.dto.payment;

import lombok.Builder;
import lombok.Data;

/**
 * Response for POST /payments/checkout. `type` tells the client how to proceed:
 *  - REDIRECT → send the browser to {@code payUrl} (MoMo / VNPay).
 *  - QR       → render {@code qrContent} as a QR and show the bank fields (VietQR manual transfer).
 * {@code timingMessage} is the localized "how soon it activates" text.
 */
@Data
@Builder
public class CheckoutResponse {
    public enum Type { REDIRECT, QR }

    private String txnRef;
    private String provider;
    private String planCode;
    private String billingCycle;
    private long amount;

    private Type type;
    private String payUrl;       // REDIRECT
    private String qrContent;    // QR (VietQR EMVCo payload)

    // VietQR display fields
    private String bankAccount;
    private String bankName;
    private String accountName;
    private String transferNote;

    private String timingMessage;
}
