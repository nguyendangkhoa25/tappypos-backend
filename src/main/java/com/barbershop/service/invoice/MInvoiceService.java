package com.barbershop.service.invoice;

import com.barbershop.model.dto.invoice.InvoiceResponse;
import com.barbershop.model.entity.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class MInvoiceService implements ExternalInvoiceService {

    @Override
    public InvoiceResponse createInvoice(Invoice invoice) {
        log.info("Creating M-Invoice for invoice {}", invoice.getId());

        // TODO: Implement M-Invoice API integration
        // This is a placeholder implementation

        return InvoiceResponse.builder()
                .success(true)
                .message("M-Invoice created successfully (placeholder)")
                .invoiceNo("M-INV-" + invoice.getId())
                .codeOfTax("TAX-" + invoice.getId())
                .transactionId("TXN-" + System.currentTimeMillis())
                .build();
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws IOException {
        log.info("Downloading M-Invoice PDF for invoice {}", invoice.getId());

        // TODO: Implement M-Invoice PDF download
        // This is a placeholder implementation

        String mockPdfContent = "M-Invoice PDF content for invoice: " + invoice.getInvoiceNumber();
        return mockPdfContent.getBytes();
    }

    @Override
    public InvoiceResponse sendEmailInvoice(Invoice invoice) {
        log.info("Sending M-Invoice email for invoice {}", invoice.getId());

        // TODO: Implement M-Invoice email sending
        // This is a placeholder implementation

        String buyerEmail = invoice.getBuyer() != null ? invoice.getBuyer().getBuyerEmail() : "N/A";

        return InvoiceResponse.builder()
                .success(true)
                .message("M-Invoice email sent to: " + buyerEmail + " (placeholder)")
                .invoiceNo(invoice.getInvoiceNumber())
                .build();
    }
}
