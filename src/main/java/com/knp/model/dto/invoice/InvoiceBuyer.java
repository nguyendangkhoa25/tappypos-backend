package com.knp.model.dto.invoice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceBuyer {
    private Long buyerId;

    private Long customerId;

    private String buyerName;
    private String buyerLegalName;

    private String buyerTaxCode;

    private String buyerAddressLine;

    private String buyerPhoneNumber;

    private String buyerFaxNumber;

    private String buyerEmail;

    private String buyerBankName;

    private String buyerBankAccount;
    private String buyerIdNo;
    private boolean visitingGuest;
}
