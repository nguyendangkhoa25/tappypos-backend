package com.barbershop.service.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceFactory {
    private final SInvoiceService sInvoiceService;
    private final MInvoiceService mInvoiceService;

    /**
     * Get the appropriate invoice service based on the type
     * @param type Invoice system type (S-INVOICE, M-INVOICE, MOCK, or null)
     * @return The corresponding ExternalInvoiceService implementation
     */
    public ExternalInvoiceService getInvoiceService(String type) {
        if (type == null || type.trim().isEmpty()) {
            log.warn("Invoice system type is null or empty, defaulting to S-INVOICE");
            return sInvoiceService;
        }

        return switch (type.toUpperCase()) {
            case "S-INVOICE" -> {
                log.info("Using S-Invoice service");
                yield sInvoiceService;
            }
            case "M-INVOICE" -> {
                log.info("Using M-Invoice service");
                yield mInvoiceService;
            }
            default -> {
                log.warn("Unknown invoice type: {}, defaulting to S-INVOICE", type);
                yield sInvoiceService;
            }
        };
    }
}
