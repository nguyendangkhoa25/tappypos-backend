package com.tappy.pos.model.dto.invoice;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdateInvoiceRequest {
    private String status;
    private String paymentType;
    private String invoiceType;
    private String invoiceSeries;
    private String notes;
    private BuyerInfoRequest buyerInfo;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuyerInfoRequest {
        private String buyerName;
        private String buyerLegalName;
        private String buyerTaxCode;
        private String buyerAddressLine;
        private String buyerPhoneNumber;
        private String buyerEmail;
        private String buyerBankName;
        private String buyerBankAccount;
        private String buyerIdNumber;
        private Boolean visitingGuest;
    }
}


