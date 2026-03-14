package com.knp.service;
import com.knp.config.AuthContext;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.CreateTenantRequest;
import com.knp.model.dto.TenantDTO;
import com.knp.model.dto.TenantStatsDTO;
import com.knp.model.dto.UpdateTenantRequest;
import com.knp.model.entity.Tenant;
import com.knp.multitenant.DatasourceManager;
import com.knp.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TenantService - Business logic for tenant operations
 * Handles tenant management and provides data transfer objects for API responses
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuthContext authContext;
    private final DatasourceManager datasourceManager;

    /**
     * Get current username for audit fields
     */
    private String getCurrentUsername() {
        String username = authContext.getCurrentUsername();
        return username != null ? username : "system";
    }

    /**
     * Get all active tenants
     * Used for tenant selection on frontend
     */
    public List<TenantDTO> getAllActiveTenants() {
        log.info("Fetching all active tenants");
        return tenantRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all tenants (active and inactive) for admin management
     */
    public List<TenantDTO> getAllTenants() {
        log.info("Fetching all tenants");
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all tenants (active and inactive) with optional search filter
     * Searches by tenant name, database name, or contact person name
     */
    public List<TenantDTO> getAllTenants(String search) {
        log.info("Fetching all tenants with search: {}", search);

        if (search == null || search.trim().isEmpty()) {
            return getAllTenants();
        }

        String searchTerm = search.trim().toLowerCase();
        return tenantRepository.findAll()
                .stream()
                .filter(tenant -> {
                    // Search in tenant name
                    if (tenant.getName() != null && tenant.getName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                    // Search in database name
                    if (tenant.getDbName() != null && tenant.getDbName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                    // Search in contact person name
                    if (tenant.getContactPersonName() != null && tenant.getContactPersonName().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                    // Search in contact person phone
                    if (tenant.getContactPersonPhone() != null && tenant.getContactPersonPhone().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                    return false;
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get dashboard stats for the master admin:
     * total / active (not expired) / inactive / expiring in ≤7 days / already expired
     */
    public TenantStatsDTO getStats() {
        List<Tenant> all = tenantRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate soonThreshold = today.plusDays(7);

        long total = all.size();
        long inactive = all.stream().filter(t -> !t.isActive()).count();
        long expired = all.stream()
                .filter(t -> t.isActive()
                        && t.getExpirationDate() != null
                        && t.getExpirationDate().isBefore(today))
                .count();
        long expiringSoon = all.stream()
                .filter(t -> t.isActive()
                        && t.getExpirationDate() != null
                        && !t.getExpirationDate().isBefore(today)
                        && !t.getExpirationDate().isAfter(soonThreshold))
                .count();
        long active = total - inactive - expired;

        return new TenantStatsDTO(total, active, inactive, expiringSoon, expired);
    }

    /**
     * Get a specific tenant by ID
     */
    public TenantDTO getTenantById(String tenantId) {
        log.info("Fetching tenant: {}", tenantId);
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> {
                    log.warn("Tenant not found: {}", tenantId);
                    return new ResourceNotFoundException("Tenant not found: " + tenantId);
                });
        return mapToDTO(tenant);
    }

    /**
     * Create a new tenant with enhanced features
     */
    public TenantDTO createTenant(CreateTenantRequest request) {
        log.info("Creating new tenant: {}", request.getTenantId());

        // Check if tenant already exists
        if (tenantRepository.findByTenantId(request.getTenantId()).isPresent()) {
            throw new RuntimeException("Tenant already exists: " + request.getTenantId());
        }

        String currentUser = getCurrentUsername();

        Tenant tenant = Tenant.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .dbName(request.getDbName())
                .active(true)
                .expirationDate(request.getExpirationDate())
                .maxUsers(request.getMaxUsers())
                .features(request.getFeatures() != null ? String.join(",", request.getFeatures()) : null)
                .subscriptionType(request.getSubscriptionType())
                .contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone())
                .contactPersonEmail(request.getContactPersonEmail())
                .contactPersonZaloId(request.getContactPersonZaloId())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .activeAt(System.currentTimeMillis())
                .activeBy(currentUser)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created successfully: {} by {}", request.getTenantId(), currentUser);
        return mapToDTO(saved);
    }

    /**
     * Update an existing tenant
     */
    public TenantDTO updateTenant(String tenantId, UpdateTenantRequest request) {
        log.info("Updating tenant: {}", tenantId);

        Tenant tenant = getTenantEntity(tenantId);
        String currentUser = getCurrentUsername();

        // Update fields if provided
        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getDbName() != null) {
            tenant.setDbName(request.getDbName());
        }
        if (request.getExpirationDate() != null) {
            tenant.setExpirationDate(request.getExpirationDate());
        }
        if (request.getMaxUsers() != null) {
            tenant.setMaxUsers(request.getMaxUsers());
        }
        if (request.getFeatures() != null) {
            tenant.setFeatures(String.join(",", request.getFeatures()));
        }
        if (request.getSubscriptionType() != null) {
            tenant.setSubscriptionType(request.getSubscriptionType());
        }
        if (request.getContactPersonName() != null) {
            tenant.setContactPersonName(request.getContactPersonName());
        }
        if (request.getContactPersonPhone() != null) {
            tenant.setContactPersonPhone(request.getContactPersonPhone());
        }
        if (request.getContactPersonEmail() != null) {
            tenant.setContactPersonEmail(request.getContactPersonEmail());
        }
        if (request.getContactPersonZaloId() != null) {
            tenant.setContactPersonZaloId(request.getContactPersonZaloId());
        }

        // Set audit field
        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);

        //Reload
        datasourceManager.reloadAllTenantDatasource();

        log.info("Tenant updated successfully: {} by {}", tenantId, currentUser);
        return mapToDTO(updated);
    }

    /**
     * Delete a tenant (soft delete by deactivating)
     */
    public void deleteTenant(String tenantId) {
        log.info("Deleting tenant: {}", tenantId);
        Tenant tenant = getTenantEntity(tenantId);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted successfully: {}", tenantId);

        // Remove datasource for deleted tenant
        datasourceManager.removeTenantDatasource(tenantId);

        //Reload
        datasourceManager.reloadAllTenantDatasource();
    }

    /**
     * Get tenant entity (with sensitive data)
     * For internal use only (not for API responses)
     */
    public Tenant getTenantEntity(String tenantId) {
        log.debug("Fetching tenant entity: {}", tenantId);
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> {
                    log.error("Tenant entity not found: {}", tenantId);
                    return new RuntimeException("Tenant not found: " + tenantId);
                });
    }

    /**
     * Deactivate a tenant
     */
    public TenantDTO deactivateTenant(String tenantId) {
        log.info("Deactivating tenant: {}", tenantId);
        Tenant tenant = getTenantEntity(tenantId);
        String currentUser = getCurrentUsername();

        tenant.setActive(false);
        tenant.setActiveAt(System.currentTimeMillis());
        tenant.setActiveBy(currentUser);
        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant deactivated: {} by {}", tenantId, currentUser);

        // Remove datasource for deactivated tenant
        datasourceManager.removeTenantDatasource(tenantId);

        //Reload all data sources
        datasourceManager.reloadAllTenantDatasource();
        return mapToDTO(updated);
    }

    /**
     * Activate a tenant
     */
    public TenantDTO activateTenant(String tenantId) {
        log.info("Activating tenant: {}", tenantId);
        Tenant tenant = getTenantEntity(tenantId);
        String currentUser = getCurrentUsername();

        tenant.setActive(true);
        tenant.setActiveAt(System.currentTimeMillis());
        tenant.setActiveBy(currentUser);
        tenant.setUpdatedBy(currentUser);

        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant activated: {} by {}", tenantId, currentUser);

        // Add datasource for activated tenant
        datasourceManager.addOrUpdateTenantDatasource(tenantId, tenant.getDbName());
        //Reload all data sources
        datasourceManager.reloadAllTenantDatasource();
        return mapToDTO(updated);
    }

    /**
     * Map Tenant entity to TenantDTO (hides sensitive data)
     */
    private TenantDTO mapToDTO(Tenant tenant) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .dbName(tenant.getDbName())
                .active(tenant.isActive())
                .expirationDate(tenant.getExpirationDate())
                .maxUsers(tenant.getMaxUsers())
                .features(tenant.getFeatures() != null ?
                    Arrays.asList(tenant.getFeatures().split(",")) : null)
                .subscriptionType(tenant.getSubscriptionType())
                .contactPersonName(tenant.getContactPersonName())
                .contactPersonPhone(tenant.getContactPersonPhone())
                .contactPersonEmail(tenant.getContactPersonEmail())
                .contactPersonZaloId(tenant.getContactPersonZaloId())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .activeAt(tenant.getActiveAt())
                .activeBy(tenant.getActiveBy())
                .createdBy(tenant.getCreatedBy())
                .updatedBy(tenant.getUpdatedBy())
                .build();
    }

    /**
     * Register datasource for a new tenant synchronously.
     * Must be called after the tenant record is committed so that provisioning
     * can immediately use the new datasource.
     */
    public void registerDatasourceSync(String tenantId, String dbName) {
        log.info("Registering datasource synchronously for tenant: {}", tenantId);
        datasourceManager.addOrUpdateTenantDatasource(tenantId, dbName);
    }

    /**
     * Create datasource for tenant in separate transaction
     * Uses REQUIRES_NEW propagation to ensure tenant is committed before datasource operations
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTenantDatasource(String tenantId, String dbName) {
        try {
            log.info("Creating datasource for tenant: {}", tenantId);
            datasourceManager.addOrUpdateTenantDatasource(tenantId, dbName);
            log.info("Datasource created for tenant: {}", tenantId);

            // Reload all datasources to make new tenant available immediately
            log.info("Reloading all tenant datasources to include newly created tenant: {}", tenantId);
            datasourceManager.reloadAllTenantDatasource();
            log.info("All tenant datasources reloaded successfully - tenant {} is now available", tenantId);
        } catch (Exception e) {
            log.error("Failed to create datasource for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create datasource: " + e.getMessage(), e);
        }
    }

    /**
     * Remove datasource for tenant in separate transaction
     * Ensures tenant deletion is committed before datasource removal
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeTenantDatasource(String tenantId) {
        try {
            datasourceManager.removeTenantDatasource(tenantId);
            log.info("Datasource removed for tenant: {}", tenantId);

            datasourceManager.reloadAllTenantDatasource();
        } catch (Exception e) {
            log.error("Failed to remove datasource for tenant: {}", tenantId, e);
        }
    }

    /**
     * Reload all tenant datasources asynchronously in background
     * Used when updating a tenant
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reloadAllDatasource() {
        log.info("Starting async datasource reload");
        try {
            datasourceManager.reloadAllTenantDatasource();
            log.info("Async datasource reload completed successfully");
        } catch (Exception e) {
            log.error("Async datasource reload failed", e);
        }
    }
}

