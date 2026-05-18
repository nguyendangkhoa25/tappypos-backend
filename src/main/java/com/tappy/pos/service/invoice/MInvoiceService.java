package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class MInvoiceService implements ExternalInvoiceService {

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("M-Invoice createInvoice (not yet implemented)");
        return InvoiceResponse.builder().success(false).message("M-Invoice not implemented").build();
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws IOException {
        log.info("M-Invoice downloadInvoicePdf (not yet implemented)");
        return new byte[0];
    }

    @Override
    public InvoiceResponse sendEmailInvoice(Invoice invoice) {
        log.info("M-Invoice sendEmailInvoice (not yet implemented)");
        return InvoiceResponse.builder().success(false).message("M-Invoice not implemented").build();
    }
}
