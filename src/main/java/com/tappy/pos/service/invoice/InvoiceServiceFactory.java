package com.tappy.pos.service.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceServiceFactory {

    private final SInvoiceService sInvoiceService;
    private final MInvoiceService mInvoiceService;

    public ExternalInvoiceService getInvoiceService(String vendor) {
        if (vendor == null) return sInvoiceService;
        return switch (vendor.toUpperCase()) {
            case "M-INVOICE", "MINVOICE", "MISA"  -> mInvoiceService;
            case "BKAV"                            -> mInvoiceService; // stub — full BKAV impl pending
            default                                -> sInvoiceService;  // SINVOICE, S-INVOICE, …
        };
    }
}
