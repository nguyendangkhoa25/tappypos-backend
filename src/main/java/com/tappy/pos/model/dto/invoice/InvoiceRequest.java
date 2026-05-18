package com.tappy.pos.model.dto.invoice;

import com.tappy.pos.model.entity.finance.Invoice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceRequest {
    private Long orderId;
    private LocalDateTime invoiceIssuedDate;
    private Invoice.InvoiceStatus status;
    private String paymentType;
    private String invoiceType;
    private String currencyCode;
    private String invoiceSeries;
    private String transactionUuid;
    private String reservationCode;
    private List<InvoiceItemRequest> itemInfo;
    private InvoiceBuyerRequest buyerInfo;
}
