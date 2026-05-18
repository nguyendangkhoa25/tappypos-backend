package com.tappy.pos.service.invoice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceFactory Unit Tests")
class InvoiceServiceFactoryTest {

    @Mock private SInvoiceService sInvoiceService;
    @Mock private MInvoiceService mInvoiceService;

    private InvoiceServiceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new InvoiceServiceFactory(sInvoiceService, mInvoiceService);
    }

    @Test
    @DisplayName("null vendor returns SInvoiceService")
    void getInvoiceService_nullVendor() {
        assertThat(factory.getInvoiceService(null)).isSameAs(sInvoiceService);
    }

    @Test
    @DisplayName("M-INVOICE vendor returns MInvoiceService")
    void getInvoiceService_mInvoice() {
        assertThat(factory.getInvoiceService("M-INVOICE")).isSameAs(mInvoiceService);
    }

    @Test
    @DisplayName("MINVOICE vendor (no hyphen) returns MInvoiceService")
    void getInvoiceService_minvoiceNoHyphen() {
        assertThat(factory.getInvoiceService("MINVOICE")).isSameAs(mInvoiceService);
    }

    @Test
    @DisplayName("m-invoice lowercase returns MInvoiceService")
    void getInvoiceService_mInvoiceLowercase() {
        assertThat(factory.getInvoiceService("m-invoice")).isSameAs(mInvoiceService);
    }

    @Test
    @DisplayName("S-INVOICE vendor returns SInvoiceService")
    void getInvoiceService_sInvoice() {
        assertThat(factory.getInvoiceService("S-INVOICE")).isSameAs(sInvoiceService);
    }

    @Test
    @DisplayName("unknown vendor falls through to SInvoiceService")
    void getInvoiceService_unknownVendor() {
        assertThat(factory.getInvoiceService("UNKNOWN_VENDOR")).isSameAs(sInvoiceService);
    }

    @Test
    @DisplayName("empty string vendor returns SInvoiceService")
    void getInvoiceService_emptyString() {
        assertThat(factory.getInvoiceService("")).isSameAs(sInvoiceService);
    }
}
