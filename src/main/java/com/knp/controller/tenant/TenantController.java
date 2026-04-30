package com.knp.controller.tenant;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.tenant.TenantDTO;
import com.knp.service.tenant.TenantService;
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
}

