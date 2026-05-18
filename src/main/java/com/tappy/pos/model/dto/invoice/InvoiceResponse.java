package com.tappy.pos.model.dto.invoice;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private boolean success;
    private String message;
    private String invoiceNo;
    private String codeOfTax;
    private String transactionId;
}

