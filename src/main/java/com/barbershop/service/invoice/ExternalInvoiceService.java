package com.barbershop.service.invoice;

import com.barbershop.model.dto.invoice.InvoiceResponse;
import com.barbershop.model.entity.Invoice;

import java.io.IOException;

/**
 * Interface for external invoice system integration
 * Supports multiple invoice systems (S-Invoice, M-Invoice, etc.)
 */
public interface ExternalInvoiceService {

    /**
     * Create/sync invoice with external system
     * @param invoice Invoice entity
     * @return Response from external system
     */
    InvoiceResponse createInvoice(Invoice invoice);

    /**
     * Download invoice PDF from external system
     * @param invoice Invoice entity
     * @return PDF file as byte array
     * @throws IOException if download fails
     */
    byte[] downloadInvoicePdf(Invoice invoice) throws IOException;

    /**
     * Send invoice via email through external system
     * @param invoice Invoice entity
     * @return Response from external system
     */
    InvoiceResponse sendEmailInvoice(Invoice invoice);
}
