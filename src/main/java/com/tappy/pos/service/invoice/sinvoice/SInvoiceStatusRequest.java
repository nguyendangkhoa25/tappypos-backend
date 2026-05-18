package com.tappy.pos.service.invoice.sinvoice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SInvoiceStatusRequest {
    private String supplierTaxCode;
    private String transactionUuid;
}
