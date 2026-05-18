package com.tappy.pos.service.invoice;

import com.tappy.pos.model.dto.invoice.CreateInvoiceRequest;
import com.tappy.pos.model.dto.invoice.CreateInputInvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceDTO;
import com.tappy.pos.model.dto.invoice.InvoiceKpiResponse;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.dto.invoice.UpdateInvoiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface InvoiceService {

    // ── Direction-scoped list / search ────────────────────────────────────────

    Page<InvoiceDTO> getAllInvoices(Pageable pageable);

    Page<InvoiceDTO> getInvoicesByStatus(String status, Pageable pageable);

    Page<InvoiceDTO> searchInvoices(String keyword, Pageable pageable);

    Page<InvoiceDTO> getOutputInvoices(Pageable pageable);

    Page<InvoiceDTO> getInputInvoices(Pageable pageable);

    Page<InvoiceDTO> getOutputInvoicesByStatus(String status, Pageable pageable);

    Page<InvoiceDTO> getInputInvoicesByStatus(String status, Pageable pageable);

    Page<InvoiceDTO> searchOutputInvoices(String keyword, Pageable pageable);

    Page<InvoiceDTO> searchInputInvoices(String keyword, Pageable pageable);

    InvoiceDTO getById(Long id);

    InvoiceDTO getByOrderId(Long orderId);

    // ── Output invoice (from orders) ──────────────────────────────────────────
    InvoiceDTO create(CreateInvoiceRequest request);

    // ── Input invoice (from vendor purchase) ──────────────────────────────────
    InvoiceDTO createInputInvoice(CreateInputInvoiceRequest request);

    /** Mark an input invoice as received/confirmed (sets status = COMPLETED) */
    InvoiceDTO confirmInputInvoice(Long id);

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
