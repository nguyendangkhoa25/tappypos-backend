package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.MasterDatabaseOnly;
import jakarta.validation.Valid;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tenant.CreateTenantRequest;
import com.tappy.pos.model.dto.tenant.ShopInfoDTO;
import com.tappy.pos.model.dto.tenant.TenantDTO;
import com.tappy.pos.model.dto.tenant.TenantStatsDTO;
import com.tappy.pos.model.dto.tenant.UpdateTenantRequest;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.tenant.Agent;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.AgentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.service.auth.RoleFeatureService;
import com.tappy.pos.service.notification.NotificationService;
import com.tappy.pos.service.tenant.ShopConfigService;
import com.tappy.pos.service.tenant.ShopInfoService;
import com.tappy.pos.service.tenant.TenantProvisioningService;
import com.tappy.pos.service.tenant.TenantSeedService;
import com.tappy.pos.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private final ShopInfoService shopInfoService;
    private final ShopConfigService shopConfigService;
    private final TenantContext tenantContext;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantSeedService tenantSeedService;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final AgentRepository agentRepository;
    private final RoleFeatureService roleFeatureService;
    private final ActivityLogService activityLogService;

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
                    request.getRoleSetups(), request.getShopAddress(),
                    request.getInitialConfig());
            try {
                tenantSeedService.seed(request.getShopType());
            } catch (Exception e) {
                log.warn("Shop-type DML seed failed for tenant {} (non-fatal): {}",
                        tenant.getTenantId(), e.getMessage());
            }
            try {
                tenantSeedService.seedShopTypeTemplates(request.getShopType());
            } catch (Exception e) {
                log.warn("Shop-type print-template seed failed for tenant {} (non-fatal): {}",
                        tenant.getTenantId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Provisioning failed for tenant {}: {}", tenant.getTenantId(), e.getMessage(), e);
            throw new RuntimeException("Tenant created but admin user provisioning failed: " + e.getMessage(), e);
        } finally {
            tenantContext.clear();
        }

        // Notify all MASTER_TENANT users asynchronously — username captured here (main thread)
        // before the async call since SecurityContextHolder is thread-local.
        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync("master", createdBy, null,
                ActivityAction.TENANT_CREATED, "TENANT", tenant.getTenantId(),
                "Tạo cửa hàng: " + tenant.getName() + " (" + tenant.getTenantId() + ")", null);
        Locale vi = new Locale("vi");
        String notifTitle = messageService.getMessage("notification.master.tenant.created.title", vi);
        String notifMsg = messageService.getMessage("notification.master.tenant.created.message", vi,
                tenant.getName(), tenant.getTenantId(), createdBy);
        notificationService.pushToRolesAsync(Notification.NotificationType.SYSTEM, notifTitle, notifMsg,
                "TENANT", tenantEntity.getId(), List.of("MASTER_TENANT"), null);

        // Notify only the new shop's admin user directly — avoids cross-tenant fan-out
        String subscriptionType = tenantEntity.getSubscriptionType() != null ? tenantEntity.getSubscriptionType() : "Trial";
        String supportInfo = buildSupportInfo(tenantEntity.getVendorId());
        String welcomeTitle = messageService.getMessage("notification.shop.welcome.title", vi);
        String welcomeMsg = messageService.getMessage("notification.shop.welcome.message", vi,
                tenant.getName(), subscriptionType, supportInfo);
        notificationService.pushSystemAsync(request.getAdminUsername(), Notification.NotificationType.SYSTEM,
                welcomeTitle, welcomeMsg, "TENANT", tenantEntity.getId(), tenant.getTenantId());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(tenant, "Tenant created successfully"));
    }

    /**
     * PUT /api/multi-tenants/{tenantId}
     * Update an existing tenant. When features change, syncs role_features in the
     * tenant DB: added features → SHOP_OWNER only; removed features → all roles.
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantDTO>> updateTenant(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantRequest request) {
        log.info("Request: Update tenant: {}", tenantId);

        // Capture current features before the update so we can diff them
        List<String> oldFeatures = tenantService.getTenantById(tenantId).getFeatures();
        if (oldFeatures == null) oldFeatures = List.of();

        TenantDTO tenant = tenantService.updateTenant(tenantId, request);

        // Sync role_features only when master admin explicitly passes a new feature list
        List<String> newFeatures = request.getFeatures();
        if (newFeatures != null) {
            Set<String> oldSet = Set.copyOf(oldFeatures);
            Set<String> newSet = Set.copyOf(newFeatures);

            List<String> added   = newFeatures.stream().filter(f -> !oldSet.contains(f)).collect(Collectors.toList());
            List<String> removed = oldFeatures.stream().filter(f -> !newSet.contains(f)).collect(Collectors.toList());

            if (!added.isEmpty() || !removed.isEmpty()) {
                Tenant tenantEntity = tenantService.getTenantEntity(tenantId);
                tenantContext.setCurrentTenant(tenantEntity);
                try {
                    roleFeatureService.syncTenantFeatureChanges(added, removed);
                    log.info("Synced role_features for tenant {}: added={} removed={}", tenantId, added, removed);
                } finally {
                    tenantContext.clear();
                }
            }
        }

        String updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync("master", updatedBy, null,
                ActivityAction.TENANT_UPDATED, "TENANT", tenantId,
                "Cập nhật cửa hàng: " + tenant.getName() + " (" + tenantId + ")", null);

        return ResponseEntity.ok(ApiResponse.success(tenant, "Tenant updated successfully"));
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
     * GET /api/multi-tenants/{tenantId}/dashboard-widgets
     * Get dashboard widget config for a specific tenant.
     */
    @GetMapping("/{tenantId}/dashboard-widgets")
    public ResponseEntity<ApiResponse<List<String>>> getTenantDashboardWidgets(@PathVariable String tenantId) {
        log.info("Request: Get dashboard widgets for tenant: {}", tenantId);
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        try {
            tenantContext.setCurrentTenant(tenant);
            List<String> widgets = shopConfigService.getDashboardWidgets();
            return ResponseEntity.ok(ApiResponse.success(widgets, "Dashboard widgets retrieved successfully"));
        } finally {
            tenantContext.clear();
        }
    }

    /**
     * PUT /api/multi-tenants/{tenantId}/dashboard-widgets
     * Update dashboard widget config for a specific tenant.
     */
    @PutMapping("/{tenantId}/dashboard-widgets")
    public ResponseEntity<ApiResponse<List<String>>> updateTenantDashboardWidgets(
            @PathVariable String tenantId,
            @RequestBody List<String> widgetIds) {
        log.info("Request: Update dashboard widgets for tenant: {}", tenantId);
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        try {
            tenantContext.setCurrentTenant(tenant);
            shopConfigService.setDashboardWidgets(widgetIds);
            return ResponseEntity.ok(ApiResponse.success(widgetIds, "Dashboard widgets updated successfully"));
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

    private String buildSupportInfo(Long vendorId) {
        if (vendorId == null) return "";
        return agentRepository.findById(vendorId).map(agent -> {
            List<String> parts = new ArrayList<>();
            if (agent.getName() != null) parts.add(agent.getName());
            if (agent.getContactPhone() != null) parts.add("☎ " + agent.getContactPhone());
            if (agent.getContactEmail() != null) parts.add("✉ " + agent.getContactEmail());
            return parts.isEmpty() ? "" : "Liên hệ hỗ trợ: " + String.join(" · ", parts);
        }).orElse("");
    }
}

