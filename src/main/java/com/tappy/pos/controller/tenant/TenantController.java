package com.tappy.pos.controller.tenant;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.TenantDTO;
import com.tappy.pos.model.dto.tenant.TenantStatusResponse;
import com.tappy.pos.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TenantController - REST API endpoints for tenant operations
 * No authentication required - these are public endpoints for tenant selection
 */
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;

    /**
     * GET /api/tenants
     * Get all active tenants for selection
     * No X-Tenant-ID header required (public endpoint)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantDTO>>> getAllTenants() {
        log.info("Request: Get all active tenants");
        List<TenantDTO> tenants = tenantService.getAllActiveTenants();
        log.info("Retrieved {} tenants", tenants.size());
        return ResponseEntity.ok(
                ApiResponse.success(tenants, "Tenants retrieved successfully")
        );
    }

    /**
     * GET /api/tenants/{tenantId}
     * Get specific tenant details
     * No X-Tenant-ID header required (public endpoint)
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantDTO>> getTenant(@PathVariable String tenantId) {
        log.info("Request: Get tenant details for: {}", tenantId);
        TenantDTO tenant = tenantService.getTenantById(tenantId);
        log.info("Retrieved tenant: {}", tenantId);
        return ResponseEntity.ok(
                ApiResponse.success(tenant, "Tenant retrieved successfully")
        );
    }

    /**
     * GET /api/tenants/{shopId}/status
     * Public endpoint for mobile app — check shop existence and activation status.
     * Returns ACTIVE, SUSPENDED, or NOT_FOUND. Always returns HTTP 200.
     */
    @GetMapping("/{shopId}/status")
    public ResponseEntity<ApiResponse<TenantStatusResponse>> checkStatus(@PathVariable String shopId) {
        log.info("Request: Check tenant status for: {}", shopId);
        TenantStatusResponse status = tenantService.checkStatus(shopId);
        return ResponseEntity.ok(
                ApiResponse.success(status, "Status retrieved successfully")
        );
    }
}

