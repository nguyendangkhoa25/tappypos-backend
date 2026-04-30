package com.knp.service.invoice;

import com.knp.model.dto.invoice.CreateInvoiceRequest;
import com.knp.model.dto.invoice.InvoiceDTO;
import com.knp.model.dto.invoice.InvoiceKpiResponse;
import com.knp.model.dto.invoice.InvoiceResponse;
import com.knp.model.dto.invoice.UpdateInvoiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface InvoiceService {

    Page<InvoiceDTO> getAllInvoices(Pageable pageable);

    Page<InvoiceDTO> getInvoicesByStatus(String status, Pageable pageable);

    Page<InvoiceDTO> searchInvoices(String keyword, Pageable pageable);

    InvoiceDTO getById(Long id);

    InvoiceDTO getByOrderId(Long orderId);

    InvoiceDTO create(CreateInvoiceRequest request);

    InvoiceDTO update(Long id, UpdateInvoiceRequest request);

    InvoiceDTO issue(Long id);

    InvoiceDTO cancel(Long id);

    void delete(Long id);

    /** Submit to external e-invoice system (SInvoice / MInvoice) */
    InvoiceDTO syncExternal(Long id);

    /** Download invoice PDF from external e-invoice system */
    byte[] downloadPdf(Long id) throws IOException;

    /** Send invoice email via external e-invoice system */
    InvoiceResponse sendEmail(Long id);

    /** KPI statistics for invoices within a date range */
    InvoiceKpiResponse getKpiSection(Long fromDateMs, Long toDateMs);
}
