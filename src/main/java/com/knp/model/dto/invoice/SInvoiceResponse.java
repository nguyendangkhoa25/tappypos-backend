package com.knp.model.dto.invoice;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SInvoiceResponse {
    private String errorCode;
    private String code;
    private String description;
    private String message;
    private Result result;

    @Getter
    @Setter
    public static class Result {
        private String supplierTaxCode;
        private String invoiceNo;
        private String transactionID;
        private String reservationCode;
        private String codeOfTax;
    }
}
