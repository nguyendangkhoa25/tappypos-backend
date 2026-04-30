package com.knp.service.invoice;

import com.knp.model.dto.invoice.InvoiceRequest;
import com.knp.model.dto.invoice.InvoiceResponse;
import com.knp.model.entity.finance.Invoice;

import java.io.IOException;

public interface ExternalInvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    byte[] downloadInvoicePdf(Invoice invoice) throws IOException;
    InvoiceResponse sendEmailInvoice(Invoice invoice);
}
