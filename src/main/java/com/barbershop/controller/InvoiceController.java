package com.barbershop.controller;

import com.barbershop.model.dto.*;
import com.barbershop.model.dto.invoice.CreateInvoiceRequest;
import com.barbershop.model.dto.invoice.InvoiceDTO;
import com.barbershop.model.dto.invoice.UpdateInvoiceRequest;
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
    public ResponseEntity<ApiResponse<InvoiceDTO>> syncWithExternalSystem(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.syncInvoiceWithExternalSystem(id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice synced successfully"));
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<ApiResponse<InvoiceDTO>> sendInvoiceEmail(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.sendInvoiceEmail(id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice email sent successfully"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        try {
            byte[] pdfBytes = invoiceService.downloadInvoicePdf(id);
            InvoiceDTO invoiceDTO = invoiceService.getInvoiceById(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"invoice_" + invoiceDTO.getInvoiceNumber() + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceDTO>> cancelInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) CancelInvoiceRequest request) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "No reason provided";
        InvoiceDTO invoice = invoiceService.cancelInvoice(id, reason);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice cancelled successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchInvoicesByStatus(
            @PathVariable String status,
            Pageable pageable) {
        Page<InvoiceDTO> invoices = invoiceService.searchInvoicesByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchInvoices(
            @RequestParam String query,
            Pageable pageable) {
        Page<InvoiceDTO> invoices = invoiceService.searchInvoices(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices found successfully"));
    }

    // Helper DTO for cancel request
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CancelInvoiceRequest {
        private String reason;
    }
}

