package com.barbershop.controller;

import com.barbershop.annotation.MasterDatabaseOnly;
import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.CreateTenantRequest;
import com.barbershop.model.dto.TenantDTO;
import com.barbershop.model.dto.UpdateTenantRequest;
import com.barbershop.model.enums.SubscriptionType;
import com.barbershop.model.enums.TenantFeature;
import com.barbershop.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MultiTenantController - REST API endpoints for tenant management
 * Admin-only endpoints for managing tenants in master database
 * All endpoints require access to master database (no X-Tenant-ID header)
 */
@RestController
@RequestMapping("/multi-tenants")
@RequiredArgsConstructor
@Slf4j
@MasterDatabaseOnly
public class MultiTenantController {

    private final TenantService tenantService;

    /**
     * GET /api/multi-tenants
     * Get all tenants (active and inactive) with optional search filter
     * Admin only
     *
     * @param search Optional search term to filter tenants by name, database, or contact info
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantDTO>>> getAllTenants(
            @RequestParam(required = false) String search) {
        log.info("Request: Get all tenants with search: {}", search);
        List<TenantDTO> tenants = tenantService.getAllTenants(search);
        return ResponseEntity.ok(
            ApiResponse.success(tenants, "Tenants retrieved successfully")
        );
    }

    /**
     * GET /api/multi-tenants/{tenantId}
     * Get a specific tenant by ID
     * Admin only
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantDTO>> getTenant(@PathVariable String tenantId) {
        log.info("Request: Get tenant: {}", tenantId);
        TenantDTO tenant = tenantService.getTenantById(tenantId);
        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant retrieved successfully")
        );
    }

    /**
     * POST /api/multi-tenants
     * Create a new tenant
     * Admin only
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TenantDTO>> createTenant(@RequestBody CreateTenantRequest request) {
        log.info("Request: Create new tenant: {}", request.getTenantId());

        TenantDTO tenant = tenantService.createTenant(request);
        log.info("Created tenant: {}", request.getTenantId());

        tenantService.createTenantDatasource(
                tenant.getTenantId(),
                tenant.getDbName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tenant, "Tenant created successfully"));
    }

    /**
     * PUT /api/multi-tenants/{tenantId}
     * Update an existing tenant
     * Admin only
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantDTO>> updateTenant(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantRequest request) {
        log.info("Request: Update tenant: {}", tenantId);
        TenantDTO tenant = tenantService.updateTenant(tenantId, request);

        log.info("Updated tenant: {}", tenantId);
        tenantService.reloadAllDatasource();

        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant updated successfully")
        );
    }

    /**
     * DELETE /api/multi-tenants/{tenantId}
     * Delete a tenant
     * Admin only
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable String tenantId) {
        log.info("Request: Delete tenant: {}", tenantId);
        tenantService.deleteTenant(tenantId);
        log.info("Deleted tenant: {}", tenantId);

        // Remove datasource asynchronously in background
        tenantService.removeTenantDatasource(tenantId);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Tenant deleted successfully")
        );
    }

    /**
     * PUT /api/multi-tenants/{tenantId}/activate
     * Activate a tenant
     * Admin only
     */
    @PutMapping("/{tenantId}/activate")
    public ResponseEntity<ApiResponse<TenantDTO>> activateTenant(@PathVariable String tenantId) {
        log.info("Request: Activate tenant: {}", tenantId);
        TenantDTO tenant = tenantService.activateTenant(tenantId);
        log.info("Activated tenant: {}", tenantId);

        tenantService.createTenantDatasource(
                tenant.getTenantId(),
                tenant.getDbName()
        );

        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant activated successfully")
        );
    }

    /**
     * PUT /api/multi-tenants/{tenantId}/deactivate
     * Deactivate a tenant
     * Admin only
     */
    @PutMapping("/{tenantId}/deactivate")
    public ResponseEntity<ApiResponse<TenantDTO>> deactivateTenant(@PathVariable String tenantId) {
        log.info("Request: Deactivate tenant: {}", tenantId);
        TenantDTO tenant = tenantService.deactivateTenant(tenantId);
        log.info("Deactivated tenant: {}", tenantId);

        tenantService.removeTenantDatasource(tenantId);

        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant deactivated successfully")
        );
    }
}

