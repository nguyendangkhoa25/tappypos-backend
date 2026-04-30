package com.knp.controller.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knp.model.dto.tenant.PrintTemplateDTO;
import com.knp.model.dto.tenant.ReceiptPreviewRequest;
import com.knp.model.dto.tenant.ReceiptTemplateConfig;
import com.knp.model.dto.tenant.SavePrintTemplateRequest;
import com.knp.model.dto.pawn.PawnStampTemplateConfig;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.service.tenant.PrintTemplateService;
import com.knp.model.dto.ApiResponse;
import com.knp.util.ReceiptHtmlBuilder;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import com.knp.annotation.RequiresFeature;

@RestController
@RequestMapping("/print-templates")
@RequiredArgsConstructor
@RequiresFeature("PRINT_TEMPLATE")
public class PrintTemplateController {

    private final PrintTemplateService service;
    private final ShopInfoRepository shopInfoRepository;
    private final ObjectMapper objectMapper;

    /** List all templates for a given type. */
    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<List<PrintTemplateDTO>>> getTemplates(@PathVariable String type) {
        return ResponseEntity.ok(ApiResponse.success(service.getTemplates(type.toUpperCase())));
    }

    /** Get a single template by id. */
    @GetMapping("/{type}/{id}")
    public ResponseEntity<ApiResponse<PrintTemplateDTO>> getTemplate(
            @PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getTemplate(id)));
    }

    /** Create a new named template. */
    @PostMapping("/{type}")
    public ResponseEntity<ApiResponse<PrintTemplateDTO>> createTemplate(
            @PathVariable String type,
            @RequestBody @Valid SavePrintTemplateRequest req) {
        PrintTemplateDTO created = service.create(type.toUpperCase(), req);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /** Update name and/or config of an existing template. */
    @PutMapping("/{type}/{id}")
    public ResponseEntity<ApiResponse<PrintTemplateDTO>> updateTemplate(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestBody @Valid SavePrintTemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    /** Mark a template as the default for its type. */
    @PutMapping("/{type}/{id}/default")
    public ResponseEntity<ApiResponse<PrintTemplateDTO>> setDefault(
            @PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.setDefault(type.toUpperCase(), id)));
    }

    /** Delete a template; the next one is auto-promoted to default if needed. */
    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable String type, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** Get the active pawn stamp config. */
    @GetMapping("/PAWN_STAMP/config")
    public ResponseEntity<ApiResponse<PawnStampTemplateConfig>> getPawnStampConfig() {
        return ResponseEntity.ok(ApiResponse.success(service.getPawnStampConfig()));
    }

    /** Render a receipt HTML preview for the supplied config (sample order data). */
    @PostMapping(value = "/preview/POS_RECEIPT", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewReceipt(@RequestBody String configJson) {
        ReceiptTemplateConfig cfg;
        try {
            cfg = objectMapper.readValue(configJson, ReceiptTemplateConfig.class);
        } catch (Exception e) {
            cfg = ReceiptTemplateConfig.defaults();
        }
        cfg.setAutoClose(false);

        ShopInfo shopInfo = shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc().orElse(null);
        return ResponseEntity.ok(ReceiptHtmlBuilder.buildPreview(buildSampleRequest(), shopInfo, cfg));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private ReceiptPreviewRequest buildSampleRequest() {
        ReceiptPreviewRequest req = new ReceiptPreviewRequest();

        ReceiptPreviewRequest.PreviewItem i1 = new ReceiptPreviewRequest.PreviewItem();
        i1.setProductName("Cà phê sữa đá"); i1.setSku("CF-SDA-001");
        i1.setQuantity(2); i1.setUnitPrice(new BigDecimal("35000"));
        i1.setLineTotal(new BigDecimal("70000")); i1.setTaxRate(new BigDecimal("10"));

        ReceiptPreviewRequest.PreviewItem i2 = new ReceiptPreviewRequest.PreviewItem();
        i2.setProductName("Bánh mì thịt"); i2.setSku("BM-THT-002");
        i2.setQuantity(1); i2.setUnitPrice(new BigDecimal("25000"));
        i2.setLineTotal(new BigDecimal("25000")); i2.setTaxRate(new BigDecimal("10"));

        req.setItems(List.of(i1, i2));
        req.setTotalDiscount(BigDecimal.ZERO);
        req.setTotal(new BigDecimal("95000"));
        req.setPaymentMethod("CASH");
        req.setAmountPaid(new BigDecimal("100000"));
        req.setChangeAmount(new BigDecimal("5000"));
        req.setCustomerName("Nguyễn Văn A");
        return req;
    }
}
