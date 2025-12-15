package com.barbershop.controller;

import com.barbershop.model.dto.*;
import com.barbershop.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDTO>> createInvoice(@RequestBody CreateInvoiceRequest request) {
        InvoiceDTO invoice = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(invoice, "Invoice created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getAllInvoices(Pageable pageable) {
        Page<InvoiceDTO> invoices = invoiceService.getAllInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoiceById(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice retrieved successfully"));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoiceByOrderId(@PathVariable Long orderId) {
        InvoiceDTO invoice = invoiceService.getInvoiceByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> updateInvoice(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceRequest request) {
        InvoiceDTO invoice = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice updated successfully"));
    }

    @PutMapping("/{id}/issue")
    public ResponseEntity<ApiResponse<InvoiceDTO>> issueInvoice(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.issueInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice issued successfully"));
    }

    @PostMapping("/{id}/sync-external")
    public ResponseEntity<ApiResponse<InvoiceDTO>> syncWithExternalSystem(
            @PathVariable Long id,
            @RequestBody SyncInvoiceRequest request) {
        InvoiceDTO invoice = invoiceService.syncWithExternalSystem(id, request);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice synced successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        try {
            InvoiceDTO invoiceDTO = invoiceService.getInvoiceById(id);
            // In production, generate PDF invoice here
            byte[] pdfBytes = new byte[0]; // Placeholder

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + invoiceDTO.getInvoiceNumber() + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchInvoicesByStatus(
            @PathVariable String status,
            Pageable pageable) {
        Page<InvoiceDTO> invoices = invoiceService.searchInvoicesByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices retrieved successfully"));
    }
}

