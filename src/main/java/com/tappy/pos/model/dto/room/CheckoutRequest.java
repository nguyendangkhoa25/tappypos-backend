package com.tappy.pos.model.dto.room;

import lombok.Data;

/** Settle and check out a stay. */
@Data
public class CheckoutRequest {
    /** Optional override of billed units (nights/hours); otherwise computed from elapsed time. */
    private Integer units;
    /** Payment method recorded on the resulting order (e.g. CASH, BANK_TRANSFER). */
    private String paymentMethod;
    private String note;

    /** 4a: issue an output e-invoice for the settlement order at checkout (best-effort). */
    private boolean issueInvoice;
    private String buyerName;
    private String buyerLegalName;
    private String buyerTaxCode;
    private String buyerAddress;
    private String buyerPhone;
    private String buyerEmail;
}
