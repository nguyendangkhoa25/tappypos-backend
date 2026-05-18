package com.tappy.pos.controller.finance;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.invoice.CreateInvoiceRequest;
import com.tappy.pos.model.dto.invoice.CreateInputInvoiceRequest;
import com.tappy.pos.model.dto.invoice.InvoiceDTO;
import com.tappy.pos.model.dto.invoice.InvoiceKpiResponse;
import com.tappy.pos.model.dto.invoice.InvoiceResponse;
import com.tappy.pos.model.dto.invoice.UpdateInvoiceRequest;
import com.tappy.pos.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@RequiresFeature("INVOICE")
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

    @GetMapping("/output")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getOutputInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /invoices/output - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getOutputInvoices(pageable), "Output invoices retrieved"));
    }

    @GetMapping("/input")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getInputInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /invoices/input - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInputInvoices(pageable), "Input invoices retrieved"));
    }

    @GetMapping("/output/status/{status}")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getOutputInvoicesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getOutputInvoicesByStatus(status, pageable), "Output invoices retrieved"));
    }

    @GetMapping("/input/status/{status}")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getInputInvoicesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInputInvoicesByStatus(status, pageable), "Input invoices retrieved"));
    }

    @GetMapping("/output/search")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchOutputInvoices(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.searchOutputInvoices(keyword, pageable), "Output invoices retrieved"));
    }

    @GetMapping("/input/search")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> searchInputInvoices(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.searchInputInvoices(keyword, pageable), "Input invoices retrieved"));
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

    @PostMapping("/input")
    public ResponseEntity<ApiResponse<InvoiceDTO>> createInputInvoice(@RequestBody CreateInputInvoiceRequest request) {
        log.info("Endpoint: POST /invoices/input");
        return ResponseEntity.ok(ApiResponse.success(invoiceService.createInputInvoice(request), "Input invoice created"));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<InvoiceDTO>> confirmInputInvoice(@PathVariable Long id) {
        log.info("Endpoint: PUT /invoices/{}/confirm", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.confirmInputInvoice(id), "Input invoice confirmed"));
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

    @PostMapping("/{id}/sync-external")
    public ResponseEntity<ApiResponse<InvoiceDTO>> syncExternal(@PathVariable Long id) {
        log.info("Endpoint: POST /invoices/{}/sync-external", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.syncExternal(id), "Invoice synced with external system"));
    }

    @GetMapping("/{id}/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws IOException {
        log.info("Endpoint: GET /invoices/{}/download-pdf", id);
        byte[] pdf = invoiceService.downloadPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<ApiResponse<InvoiceResponse>> sendEmail(@PathVariable Long id) {
        log.info("Endpoint: POST /invoices/{}/send-email", id);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.sendEmail(id), "Email sent"));
    }

    @PostMapping("/kpi-section")
    public ResponseEntity<ApiResponse<InvoiceKpiResponse>> getKpiSection(@RequestBody Map<String, Long> request) {
        log.info("Endpoint: POST /invoices/kpi-section");
        Long fromDate = request.get("fromDate");
        Long toDate = request.get("toDate");
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getKpiSection(fromDate, toDate), "KPI retrieved"));
    }
}
