package com.knp.service;

import com.knp.model.dto.invoice.CreateInvoiceRequest;
import com.knp.model.dto.invoice.InvoiceDTO;
import com.knp.model.dto.invoice.UpdateInvoiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
