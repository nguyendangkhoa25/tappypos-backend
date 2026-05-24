package com.tappy.pos.controller.mobile;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.ConnectZaloOaRequest;
import com.tappy.pos.model.dto.tenant.ZaloOaStatusDTO;
import com.tappy.pos.service.tenant.ZaloOaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Manages the per-tenant Zalo Official Account connection.
 * All endpoints are gated by the APPOINTMENT feature.
 */
@Slf4j
@RestController
@RequestMapping("/shop-config/zalo-oa")
@RequiredArgsConstructor
@RequiresFeature("APPOINTMENT")
public class ZaloOaController {

    private final ZaloOaService zaloOaService;

    /**
     * GET /shop-config/zalo-oa
     * Returns the current connection status (connected, appId, expiry).
     * Never returns secret or token values.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ZaloOaStatusDTO>> getStatus() {
        log.info("Endpoint: GET /shop-config/zalo-oa");
        return ResponseEntity.ok(ApiResponse.success(zaloOaService.getStatus(), "OK"));
    }

    /**
     * POST /shop-config/zalo-oa
     * Connects the tenant's Zalo OA using App ID + App Secret.
     * Immediately fetches and validates an access token — returns 400 if
     * Zalo rejects the credentials.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ZaloOaStatusDTO>> connect(
            @Valid @RequestBody ConnectZaloOaRequest request) {
        log.info("Endpoint: POST /shop-config/zalo-oa — appId={}", request.getAppId());
        ZaloOaStatusDTO status = zaloOaService.connect(request);
        return ResponseEntity.ok(ApiResponse.success(status, "Zalo OA connected"));
    }

    /**
     * DELETE /shop-config/zalo-oa
     * Clears all Zalo OA credentials. The tenant then falls back to the
     * platform's global ZNS token.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        log.info("Endpoint: DELETE /shop-config/zalo-oa");
        zaloOaService.disconnect();
        return ResponseEntity.ok(ApiResponse.success(null, "Zalo OA disconnected"));
    }
}
