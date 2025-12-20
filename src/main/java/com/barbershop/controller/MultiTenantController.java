package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.TenantDTO;
import com.barbershop.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TenantController - REST API endpoints for tenant operations
 * No authentication required - these are public endpoints for tenant selection
 */
@RestController
@RequestMapping("/multi-tenants")
@RequiredArgsConstructor
@Slf4j
public class MultiTenantController {

    private final TenantService tenantService;

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

