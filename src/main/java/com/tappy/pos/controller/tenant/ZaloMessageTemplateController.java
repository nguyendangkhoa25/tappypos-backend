package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.SaveZaloMessageTemplateRequest;
import com.tappy.pos.model.dto.tenant.ZaloMessageTemplateDTO;
import com.tappy.pos.service.tenant.ZaloMessageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shop-config/zalo-templates")
@RequiredArgsConstructor
@RequiresFeature("APPOINTMENT")
public class ZaloMessageTemplateController {

    private final ZaloMessageTemplateService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ZaloMessageTemplateDTO>>> list(
            @RequestParam(defaultValue = ZaloMessageTemplateService.APPOINTMENT_REMINDER) String type) {
        return ResponseEntity.ok(ApiResponse.success(service.list(type), "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ZaloMessageTemplateDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ZaloMessageTemplateDTO>> create(
            @RequestParam(defaultValue = ZaloMessageTemplateService.APPOINTMENT_REMINDER) String type,
            @Valid @RequestBody SaveZaloMessageTemplateRequest req) {
        log.info("POST /shop-config/zalo-templates?type={}", type);
        return ResponseEntity.ok(ApiResponse.success(service.create(type, req), "Zalo template created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ZaloMessageTemplateDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveZaloMessageTemplateRequest req) {
        log.info("PUT /shop-config/zalo-templates/{}", id);
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Zalo template updated"));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<ZaloMessageTemplateDTO>> setDefault(
            @PathVariable Long id,
            @RequestParam(defaultValue = ZaloMessageTemplateService.APPOINTMENT_REMINDER) String type) {
        log.info("PUT /shop-config/zalo-templates/{}/default", id);
        return ResponseEntity.ok(ApiResponse.success(service.setDefault(type, id), "Default template updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /shop-config/zalo-templates/{}", id);
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Zalo template deleted"));
    }
}
