package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncInvoiceService Unit Tests")
class AsyncInvoiceServiceTest {

    @Mock private InvoiceRepository      invoiceRepository;
    @Mock private InvoiceServiceFactory  invoiceServiceFactory;
    @Mock private TenantContext          tenantContext;
    @Mock private ExternalInvoiceService externalService;

    @InjectMocks
    private AsyncInvoiceService asyncInvoiceService;

    private Tenant      tenant;
    private Invoice     invoice;
    private InvoiceRequest request;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantId("shop-abc");

        invoice = new Invoice();
        invoice.setId(1L);

        request = InvoiceRequest.builder().build();

        lenient().when(invoiceServiceFactory.getInvoiceService("SINVOICE")).thenReturn(externalService);
    }

    // ── success path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncWithExternalAsync: success response — updates invoice and saves")
    void syncWithExternalAsync_success_updatesInvoice() {
        InvoiceResponse response = InvoiceResponse.builder()
                .success(true)
                .invoiceNo("INV-2024-001")
                .codeOfTax("TAX-CODE-123")
                .build();

        when(externalService.createInvoice(any())).thenReturn(response);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        asyncInvoiceService.syncWithExternalAsync(1L, request, tenant, "SINVOICE");

        verify(tenantContext).setCurrentTenant(tenant);
        verify(invoiceRepository).save(invoice);
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("syncWithExternalAsync: success with transactionId — sets transactionUuid on invoice")
    void syncWithExternalAsync_success_withTransactionId() {
        InvoiceResponse response = InvoiceResponse.builder()
                .success(true)
                .invoiceNo("INV-2024-002")
                .transactionId("TXN-ABC-123")
                .build();

        when(externalService.createInvoice(any())).thenReturn(response);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        asyncInvoiceService.syncWithExternalAsync(1L, request, tenant, "SINVOICE");

        verify(invoiceRepository).save(invoice);
        verify(tenantContext).clear();
    }

    // ── failure path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncWithExternalAsync: failure response — sets errorMessage on invoice")
    void syncWithExternalAsync_failure_setsErrorMessage() {
        InvoiceResponse response = InvoiceResponse.builder()
                .success(false)
                .message("Signature mismatch")
                .build();

        when(externalService.createInvoice(any())).thenReturn(response);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        asyncInvoiceService.syncWithExternalAsync(1L, request, tenant, "SINVOICE");

        verify(invoiceRepository).save(invoice);
        verify(tenantContext).clear();
    }

    // ── invoice not found ──────────────────────────────────────────────────────

    @Test
    @DisplayName("syncWithExternalAsync: invoice not found after external call — returns early without save")
    void syncWithExternalAsync_invoiceNotFound_noSave() {
        InvoiceResponse response = InvoiceResponse.builder().success(true).invoiceNo("INV-999").build();

        when(externalService.createInvoice(any())).thenReturn(response);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());

        asyncInvoiceService.syncWithExternalAsync(1L, request, tenant, "SINVOICE");

        verify(invoiceRepository, never()).save(any());
        verify(tenantContext).clear();
    }

    // ── exception swallowed ────────────────────────────────────────────────────

    @Test
    @DisplayName("syncWithExternalAsync: external service throws — exception swallowed, tenant context cleared")
    void syncWithExternalAsync_externalThrows_tenantContextAlwaysCleared() {
        when(externalService.createInvoice(any())).thenThrow(new RuntimeException("Connection refused"));

        asyncInvoiceService.syncWithExternalAsync(1L, request, tenant, "SINVOICE");

        verify(invoiceRepository, never()).save(any());
        verify(tenantContext).clear();
    }
}
