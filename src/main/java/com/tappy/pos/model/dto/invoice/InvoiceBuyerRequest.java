package com.tappy.pos.model.dto.invoice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceBuyerRequest {
    private Long buyerId;
    private Long invoiceId;
    private Long customerId;
    private String buyerName;
    private String buyerIdNo;
    private String buyerLegalName;
    private String buyerTaxCode;
    private String buyerAddressLine;
    private String buyerPhoneNumber;
    private String buyerFaxNumber;
    private String buyerEmail;
    private String buyerBankName;
    private String buyerBankAccount;
    private boolean visitingGuest;
}
