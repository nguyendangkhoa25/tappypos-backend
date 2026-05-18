package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MInvoiceService Unit Tests")
class MInvoiceServiceTest {

    private MInvoiceService mInvoiceService;

    @BeforeEach
    void setUp() {
        mInvoiceService = new MInvoiceService();
    }

    @Test
    @DisplayName("createInvoice returns not-implemented response")
    void createInvoice_returnsNotImplemented() {
        InvoiceRequest request = InvoiceRequest.builder().build();

        InvoiceResponse response = mInvoiceService.createInvoice(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("M-Invoice");
    }

    @Test
    @DisplayName("downloadInvoicePdf returns empty byte array")
    void downloadInvoicePdf_returnsEmptyArray() throws IOException {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-TEST-001")
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();

        byte[] result = mInvoiceService.downloadInvoicePdf(invoice);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("sendEmailInvoice returns not-implemented response")
    void sendEmailInvoice_returnsNotImplemented() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-TEST-002")
                .status(Invoice.InvoiceStatus.COMPLETED)
                .build();

        InvoiceResponse response = mInvoiceService.sendEmailInvoice(invoice);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("M-Invoice");
    }
}
