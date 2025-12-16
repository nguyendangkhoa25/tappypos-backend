package com.barbershop.service;

import com.barbershop.model.dto.invoice.CreateInvoiceRequest;
import com.barbershop.model.dto.invoice.InvoiceDTO;
import com.barbershop.model.dto.invoice.SyncInvoiceRequest;
import com.barbershop.model.dto.invoice.UpdateInvoiceRequest;
import com.barbershop.model.entity.Invoice;
import com.barbershop.model.entity.Order;
import com.barbershop.repository.InvoiceRepository;
import com.barbershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        Order order = orderRepository.findByIdActive(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if invoice already exists for this order
        invoiceRepository.findByOrderId(request.getOrderId())
                .ifPresent(inv -> {
                    throw new RuntimeException("Invoice already exists for this order");
                });

        String invoiceNumber = generateInvoiceNumber();
        BigDecimal tax = request.getTax() != null ? request.getTax() : BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber(invoiceNumber)
                .totalAmount(order.getTotalAmount())
                .tax(tax)
                .status(Invoice.InvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        order.setInvoice(invoice);
        orderRepository.save(order);

        return mapToDTO(saved);
    }

    public Page<InvoiceDTO> getAllInvoices(Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findAllActive(pageable);
        return invoices.map(this::mapToDTO);
    }

    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        return mapToDTO(invoice);
    }

    public InvoiceDTO getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order: " + orderId));
        return mapToDTO(invoice);
    }

    public InvoiceDTO updateInvoice(Long id, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        if (request.getStatus() != null) {
            invoice.setStatus(Invoice.InvoiceStatus.valueOf(request.getStatus()));
        }

        if (request.getTax() != null) {
            invoice.setTax(request.getTax());
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        Invoice updated = invoiceRepository.save(invoice);
        return mapToDTO(updated);
    }

    public InvoiceDTO issueInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
        Invoice updated = invoiceRepository.save(invoice);
        return mapToDTO(updated);
    }

    public InvoiceDTO syncWithExternalSystem(Long id, SyncInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        try {
            // Call external API to sync invoice
            String externalInvoiceId = callExternalInvoiceSystem(invoice, request);
            invoice.syncWithExternal(externalInvoiceId);
            Invoice updated = invoiceRepository.save(invoice);
            return mapToDTO(updated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync with external system: " + e.getMessage());
        }
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        invoice.softDelete();
        invoiceRepository.save(invoice);
    }

    public Page<InvoiceDTO> searchInvoicesByStatus(String status, Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findByStatus(status, pageable);
        return invoices.map(this::mapToDTO);
    }

    private String generateInvoiceNumber() {
        // Format: INV-20250101-UUID (first 8 chars)
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + date + "-" + uuid;
    }

    private String callExternalInvoiceSystem(Invoice invoice, SyncInvoiceRequest request) {
        // This is a placeholder for external API integration
        // In production, you would call the actual external system API
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("invoiceNumber", invoice.getInvoiceNumber());
            payload.put("totalAmount", invoice.getTotalAmount());
            payload.put("tax", invoice.getTax());
            payload.put("orderId", invoice.getOrder().getId());
            payload.put("timestamp", LocalDateTime.now());

            // Example: RestTemplate call to external API
            // return restTemplate.postForObject(request.getExternalSystemUrl() + "/invoices",
            //         payload, String.class);

            // For now, generate a mock external ID
            return "EXT-" + UUID.randomUUID().toString();
        } catch (Exception e) {
            throw new RuntimeException("External system call failed", e);
        }
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrder().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getTotalAmount())
                .tax(invoice.getTax())
                .status(invoice.getStatus().name())
                .externalInvoiceId(invoice.getExternalInvoiceId())
                .externalSyncAt(invoice.getExternalSyncAt())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}

