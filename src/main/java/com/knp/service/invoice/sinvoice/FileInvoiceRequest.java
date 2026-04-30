package com.knp.service.invoice.sinvoice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInvoiceRequest {
    private String supplierTaxCode;
    private String invoiceNo;
    private String templateCode;
    private String transactionUuid;
    private String fileType;
}
