package com.tappy.pos.controller.tax;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tax.TaxDeclarationDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationRequest;
import com.tappy.pos.model.dto.tax.TaxEstimateDTO;
import com.tappy.pos.model.dto.tax.TaxRateCatalogDTO;
import com.tappy.pos.service.tax.TaxDeclarationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tax-declarations")
@RequiredArgsConstructor
@RequiresFeature("TAX_DECLARATION")
public class TaxDeclarationController {

    private final TaxDeclarationService taxDeclarationService;

    @GetMapping("/rate-catalog")
    public ResponseEntity<ApiResponse<List<TaxRateCatalogDTO>>> getRateCatalog() {
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.getRateCatalog(), "Rate catalog retrieved"));
    }

    @GetMapping("/estimate")
    public ResponseEntity<ApiResponse<TaxEstimateDTO>> estimate(
            @RequestParam(defaultValue = "QUARTER") String periodType,
            @RequestParam int year,
            @RequestParam int number) {
        log.info("Endpoint: GET /tax-declarations/estimate - {} {}/{}", periodType, number, year);
        return ResponseEntity.ok(ApiResponse.success(
                taxDeclarationService.estimate(periodType, year, number), "Estimate computed"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaxDeclarationDTO>>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.list(year, pageable), "Declarations retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxDeclarationDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.getById(id), "Declaration retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaxDeclarationDTO>> create(@RequestBody TaxDeclarationRequest request) {
        log.info("Endpoint: POST /tax-declarations");
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.createDraft(request), "Declaration created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxDeclarationDTO>> update(
            @PathVariable Long id, @RequestBody TaxDeclarationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.update(id, request), "Declaration updated"));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<TaxDeclarationDTO>> finalizeDeclaration(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.finalizeDeclaration(id), "Declaration finalized"));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<TaxDeclarationDTO>> submit(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String govRef = body != null ? body.get("govRefNumber") : null;
        return ResponseEntity.ok(ApiResponse.success(taxDeclarationService.markSubmitted(id, govRef), "Declaration submitted"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        taxDeclarationService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Declaration cancelled"));
    }

    @GetMapping("/{id}/export/html")
    public ResponseEntity<byte[]> exportHtml(@PathVariable Long id) {
        byte[] html = taxDeclarationService.exportPrintableHtml(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
        return ResponseEntity.ok().headers(headers).body(html);
    }
}
