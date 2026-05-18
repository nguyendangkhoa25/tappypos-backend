package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.InvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.entity.finance.Invoice;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceServiceFactory invoiceServiceFactory;
    private final TenantContext tenantContext;

    /**
     * Calls the external e-invoice provider in a background thread.
     * Tenant is passed explicitly because TenantContext (ThreadLocal) is not inherited by async threads.
     */
    @Async
    public void syncWithExternalAsync(Long invoiceId, InvoiceRequest request, Tenant tenant, String vendor) {
        try {
            tenantContext.setCurrentTenant(tenant);
            log.info("Async e-invoice sync started: invoiceId={}, tenant={}, vendor={}",
                    invoiceId, tenant.getTenantId(), vendor);

            ExternalInvoiceService externalService = invoiceServiceFactory.getInvoiceService(vendor);
            InvoiceResponse response = externalService.createInvoice(request);

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.warn("Async e-invoice sync: invoice {} not found after external call", invoiceId);
                return;
            }

            if (response.isSuccess()) {
                invoice.setExternalInvoiceId(response.getInvoiceNo());
                invoice.setCodeOfTax(response.getCodeOfTax());
                invoice.setExternalSyncAt(LocalDateTime.now());
                log.info("Async e-invoice sync success: invoiceId={}, invoiceNo={}", invoiceId, response.getInvoiceNo());
            } else {
                log.error("Async e-invoice sync failed: invoiceId={}, reason={}", invoiceId, response.getMessage());
            }
            if (response.getTransactionId() != null) {
                invoice.setTransactionUuid(response.getTransactionId());
            }
            if (!response.isSuccess() && response.getMessage() != null) {
                invoice.setErrorMessage(response.getMessage());
            }
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.error("Async e-invoice sync threw exception for invoiceId={}", invoiceId, e);
        } finally {
            tenantContext.clear();
        }
    }
}
