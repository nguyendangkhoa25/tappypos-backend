package com.tappy.pos.service.invoice.sinvoice;

import lombok.Data;

@Data
public class SInvoiceFileResponse {
    private int errorCode;
    private String description;
    private boolean paymentStatus;
    private String fileName;
    private String fileToBytes;
}
