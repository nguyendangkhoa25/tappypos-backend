package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.TenantDTO;
import com.barbershop.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
     * POST /api/tenants
     * Create a new tenant
     * Admin only (no header check because it's for admin use)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TenantDTO>> createTenant(@RequestBody Map<String, String> request) {
        log.info("Request: Create new tenant: {}", request.get("tenantId"));

        TenantDTO tenant = tenantService.createTenant(
            request.get("tenantId"),
            request.get("name"),
            request.get("dbUrl"),
            request.get("dbName"),
            request.get("dbUsername"),
            request.get("dbPassword")
        );

        log.info("Created tenant: {}", request.get("tenantId"));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tenant, "Tenant created successfully"));
    }

    /**
     * PUT /api/tenants/{tenantId}/deactivate
     * Deactivate a tenant
     */
    @PutMapping("/{tenantId}/deactivate")
    public ResponseEntity<ApiResponse<TenantDTO>> deactivateTenant(@PathVariable String tenantId) {
        log.info("Request: Deactivate tenant: {}", tenantId);
        TenantDTO tenant = tenantService.deactivateTenant(tenantId);
        log.info("Deactivated tenant: {}", tenantId);
        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant deactivated successfully")
        );
    }

    /**
     * PUT /api/tenants/{tenantId}/activate
     * Activate a tenant
     */
    @PutMapping("/{tenantId}/activate")
    public ResponseEntity<ApiResponse<TenantDTO>> activateTenant(@PathVariable String tenantId) {
        log.info("Request: Activate tenant: {}", tenantId);
        TenantDTO tenant = tenantService.activateTenant(tenantId);
        log.info("Activated tenant: {}", tenantId);
        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant activated successfully")
        );
    }
}

