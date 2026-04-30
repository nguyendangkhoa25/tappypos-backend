package com.knp.service.invoice.sinvoice;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class SInvoiceStatusResponse {
    private String errorCode;
    private String description;
    private String transactionUuid;
    private List<StatusResult> result;


    @Getter
    @Setter
    public static class StatusResult {
        private String supplierTaxCode;
        private String invoiceNo;
        private String reservationCode;
        private Long issueDate;
        private String status;
        private String exchangeStatus;
        private String exchangeDes;
        private String codeOfTax;
    }
}
