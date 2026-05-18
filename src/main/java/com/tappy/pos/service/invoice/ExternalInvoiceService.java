package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;

import java.io.IOException;

public interface ExternalInvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    byte[] downloadInvoicePdf(Invoice invoice) throws IOException;
    InvoiceResponse sendEmailInvoice(Invoice invoice);
}
