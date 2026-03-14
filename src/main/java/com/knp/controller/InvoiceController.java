package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.invoice.CreateInvoiceRequest;
import com.knp.model.dto.invoice.InvoiceDTO;
import com.knp.model.dto.invoice.UpdateInvoiceRequest;
import com.knp.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /invoices - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAllInvoices(pageable), "Invoices retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getInvoicesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /invoices/status/{} - page: {}, size: {}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoicesByStatus(status, pageable), "Invoices retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchInvoices(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /invoices/search - keyword: {}", keyword);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.searchInvoices(keyword, pageable), "Invoices retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoiceById(@PathVariable Long id) {
        log.info("Endpoint: GET /invoices/{}", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id), "Invoice retrieved successfully"));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoiceByOrderId(@PathVariable Long orderId) {
        log.info("Endpoint: GET /invoices/order/{}", orderId);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByOrderId(orderId), "Invoice retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDTO>> createInvoice(@RequestBody CreateInvoiceRequest request) {
        log.info("Endpoint: POST /invoices");
        InvoiceDTO invoice = invoiceService.create(request);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> updateInvoice(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceRequest request) {
        log.info("Endpoint: PUT /invoices/{}", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.update(id, request), "Invoice updated successfully"));
    }

    @PutMapping("/{id}/issue")
    public ResponseEntity<ApiResponse<InvoiceDTO>> issueInvoice(@PathVariable Long id) {
        log.info("Endpoint: PUT /invoices/{}/issue", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.issue(id), "Invoice issued successfully"));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceDTO>> cancelInvoice(@PathVariable Long id) {
        log.info("Endpoint: PUT /invoices/{}/cancel", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.cancel(id), "Invoice cancelled successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        log.info("Endpoint: DELETE /invoices/{}", id);
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted successfully"));
    }
}
