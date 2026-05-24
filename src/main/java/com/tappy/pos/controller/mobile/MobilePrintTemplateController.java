package com.tappy.pos.controller.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.MobileCreatePrintTemplateRequest;
import com.tappy.pos.model.dto.tenant.MobilePrintTemplateResponse;
import com.tappy.pos.model.dto.tenant.MobileUpdatePrintTemplateRequest;
import com.tappy.pos.model.dto.tenant.PrintTemplateDTO;
import com.tappy.pos.model.dto.tenant.SavePrintTemplateRequest;
import com.tappy.pos.service.tenant.PrintTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mobile-specific print-template CRUD.
 *
 * Differences from the web controller:
 *  – {@code config} is serialised/deserialised as a JSON object (Jackson {@link JsonNode}),
 *    not a raw JSON string, so the mobile TypeScript model can handle it directly.
 *  – {@code type} is exposed as a flat string field (web uses {@code templateType}).
 *  – {@code id} is returned as a String (mobile TypeScript type uses string IDs).
 *  – {@code updatedAt} is returned as an ISO-8601 string.
 */
@Slf4j
@RestController
@RequestMapping("/shop-config/print-templates")
@RequiresFeature("PRINT_TEMPLATE")
@RequiredArgsConstructor
public class MobilePrintTemplateController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PrintTemplateService printTemplateService;
    private final ObjectMapper objectMapper;

    // ── GET /shop-config/print-templates ──────────────────────────────────

    /**
     * List all print templates (all types).
     * GET /api/v1/shop-config/print-templates
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MobilePrintTemplateResponse>>> getPrintTemplates() {
        log.info("GET /shop-config/print-templates - list all");
        List<MobilePrintTemplateResponse> templates = printTemplateService.getAllTemplates()
                .stream().map(this::toMobileResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(templates, "Print templates retrieved successfully"));
    }

    // ── GET /shop-config/print-templates/{id} ─────────────────────────────

    /**
     * Get a single print template by id.
     * GET /api/v1/shop-config/print-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MobilePrintTemplateResponse>> getPrintTemplate(
            @PathVariable Long id) {
        log.info("GET /shop-config/print-templates/{}", id);
        MobilePrintTemplateResponse template = toMobileResponse(printTemplateService.getTemplate(id));
        return ResponseEntity.ok(ApiResponse.success(template, "Print template retrieved successfully"));
    }

    // ── POST /shop-config/print-templates ─────────────────────────────────

    /**
     * Create a new print template.
     * POST /api/v1/shop-config/print-templates
     *
     * If the caller sets {@code isDefault: true} and there are already templates of that type,
     * the new template is promoted to default after creation.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MobilePrintTemplateResponse>> createPrintTemplate(
            @RequestBody @Valid MobileCreatePrintTemplateRequest request) {
        log.info("POST /shop-config/print-templates - type: {}, name: {}", request.getType(), request.getName());

        SavePrintTemplateRequest saveReq = toSaveRequest(request.getName(), request.getConfig());
        PrintTemplateDTO created = printTemplateService.create(request.getType(), saveReq);

        // Promote to default when caller asked for it and the service didn't auto-default it.
        if (request.isDefault() && !created.isDefault()) {
            created = printTemplateService.setDefault(request.getType(), created.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toMobileResponse(created), "Print template created successfully"));
    }

    // ── PUT /shop-config/print-templates/{id} ─────────────────────────────

    /**
     * Update name and config of an existing template.
     * PUT /api/v1/shop-config/print-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MobilePrintTemplateResponse>> updatePrintTemplate(
            @PathVariable Long id,
            @RequestBody @Valid MobileUpdatePrintTemplateRequest request) {
        log.info("PUT /shop-config/print-templates/{} - name: {}", id, request.getName());

        SavePrintTemplateRequest saveReq = toSaveRequest(request.getName(), request.getConfig());
        PrintTemplateDTO updated = printTemplateService.update(id, saveReq);
        return ResponseEntity.ok(ApiResponse.success(toMobileResponse(updated), "Print template updated successfully"));
    }

    // ── DELETE /shop-config/print-templates/{id} ──────────────────────────

    /**
     * Soft-delete a print template.
     * DELETE /api/v1/shop-config/print-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePrintTemplate(@PathVariable Long id) {
        log.info("DELETE /shop-config/print-templates/{}", id);
        printTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Print template deleted successfully"));
    }

    // ── PUT /shop-config/print-templates/{id}/default ─────────────────────

    /**
     * Set a template as the default for its type.
     * PUT /api/v1/shop-config/print-templates/{id}/default
     *
     * The template's type is resolved server-side so the mobile doesn't need to pass it.
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<MobilePrintTemplateResponse>> setDefaultTemplate(
            @PathVariable Long id) {
        log.info("PUT /shop-config/print-templates/{}/default", id);

        // Look up the template first to retrieve its type (required by setDefault).
        PrintTemplateDTO existing = printTemplateService.getTemplate(id);
        PrintTemplateDTO updated = printTemplateService.setDefault(existing.getTemplateType(), id);
        return ResponseEntity.ok(ApiResponse.success(toMobileResponse(updated), "Default template updated successfully"));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    /**
     * Convert a {@link PrintTemplateDTO} (web shape) to a {@link MobilePrintTemplateResponse}.
     * Parses {@code configJson} (raw string) into a {@link JsonNode} object for the mobile client.
     */
    private MobilePrintTemplateResponse toMobileResponse(PrintTemplateDTO dto) {
        JsonNode configNode;
        try {
            configNode = objectMapper.readTree(dto.getConfigJson());
        } catch (Exception e) {
            log.warn("Failed to parse configJson for template id={}: {}", dto.getId(), e.getMessage());
            configNode = objectMapper.createObjectNode();
        }
        String updatedAt = dto.getUpdatedAt() != null ? dto.getUpdatedAt().format(ISO) : null;
        return new MobilePrintTemplateResponse(
                String.valueOf(dto.getId()),
                dto.getName(),
                dto.getTemplateType(),
                dto.isDefault(),
                configNode,
                updatedAt
        );
    }

    /**
     * Build a {@link SavePrintTemplateRequest} from the name and a {@link JsonNode} config.
     * Serialises the JsonNode back to a JSON string for the service layer.
     */
    private SavePrintTemplateRequest toSaveRequest(String name, JsonNode config) {
        String configJson;
        try {
            configJson = config != null
                    ? objectMapper.writeValueAsString(config)
                    : "{}";
        } catch (Exception e) {
            log.warn("Failed to serialise config JsonNode: {}", e.getMessage());
            configJson = "{}";
        }
        SavePrintTemplateRequest req = new SavePrintTemplateRequest();
        req.setName(name);
        req.setConfigJson(configJson);
        return req;
    }
}
