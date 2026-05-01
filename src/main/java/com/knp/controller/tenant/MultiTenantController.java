package com.knp.controller.tenant;

import com.knp.annotation.MasterDatabaseOnly;
import jakarta.validation.Valid;
import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.tenant.CreateTenantRequest;
import com.knp.model.dto.tenant.ShopInfoDTO;
import com.knp.model.dto.tenant.TenantDTO;
import com.knp.model.dto.tenant.TenantStatsDTO;
import com.knp.model.dto.tenant.UpdateTenantRequest;
import com.knp.model.entity.tenant.Tenant;
import com.knp.multitenant.TenantContext;
import com.knp.service.tenant.ShopInfoService;
import com.knp.service.tenant.TenantProvisioningService;
import com.knp.service.tenant.TenantSeedService;
import com.knp.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final ShopInfoService shopInfoService;
    private final TenantContext tenantContext;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantSeedService tenantSeedService;

    /**
     * GET /api/multi-tenants/stats
     * Returns aggregate counts for the master dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TenantStatsDTO>> getStats() {
        log.info("Request: Get tenant stats");
        TenantStatsDTO stats = tenantService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved successfully"));
    }

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
    public ResponseEntity<ApiResponse<TenantDTO>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        log.info("Request: Create new tenant: {}", request.getTenantId());

        TenantDTO tenant = tenantService.createTenant(request);
        log.info("Created tenant record: {}", request.getTenantId());

        // Provision default data (roles, features, admin user, shop info, walk-in customer)
        Tenant tenantEntity = tenantService.getTenantEntity(tenant.getTenantId());
        tenantContext.setCurrentTenant(tenantEntity);
        try {
            tenantProvisioningService.provision(tenantEntity,
                    request.getAdminUsername(), request.getAdminPassword(),
                    request.getRoleSetups(), request.getShopAddress());
            try {
                tenantSeedService.seed(request.getShopType());
            } catch (Exception e) {
                log.warn("Shop-type DML seed failed for tenant {} (non-fatal): {}",
                        tenant.getTenantId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Provisioning failed for tenant {}: {}", tenant.getTenantId(), e.getMessage(), e);
            throw new RuntimeException("Tenant created but admin user provisioning failed: " + e.getMessage(), e);
        } finally {
            tenantContext.clear();
        }

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
        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant activated successfully")
        );
    }

    /**
     * GET /api/multi-tenants/{tenantId}/shop-info
     * Get shop info for a specific tenant (master admin view/setup).
     * Temporarily routes to the tenant's database.
     */
    @GetMapping("/{tenantId}/shop-info")
    public ResponseEntity<ApiResponse<ShopInfoDTO>> getTenantShopInfo(@PathVariable String tenantId) {
        log.info("Request: Get shop info for tenant: {}", tenantId);
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        try {
            tenantContext.setCurrentTenant(tenant);
            ShopInfoDTO shopInfo = shopInfoService.getShopInfo();
            return ResponseEntity.ok(ApiResponse.success(shopInfo, "Shop info retrieved successfully"));
        } finally {
            tenantContext.clear();
        }
    }

    /**
     * PUT /api/multi-tenants/{tenantId}/shop-info
     * Update shop info for a specific tenant (master admin setup).
     * Temporarily routes to the tenant's database.
     */
    @PutMapping("/{tenantId}/shop-info")
    public ResponseEntity<ApiResponse<ShopInfoDTO>> updateTenantShopInfo(
            @PathVariable String tenantId,
            @RequestBody ShopInfoDTO shopInfoDTO) {
        log.info("Request: Update shop info for tenant: {}", tenantId);
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        try {
            tenantContext.setCurrentTenant(tenant);
            ShopInfoDTO updated = shopInfoService.updateShopInfo(shopInfoDTO);
            return ResponseEntity.ok(ApiResponse.success(updated, "Shop info updated successfully"));
        } finally {
            tenantContext.clear();
        }
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
        return ResponseEntity.ok(
            ApiResponse.success(tenant, "Tenant deactivated successfully")
        );
    }
}

