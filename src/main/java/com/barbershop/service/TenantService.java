package com.barbershop.service;

import com.barbershop.model.dto.TenantDTO;
import com.barbershop.model.entity.Tenant;
import com.barbershop.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Get a specific tenant by ID
     */
    public TenantDTO getTenantById(String tenantId) {
        log.info("Fetching tenant: {}", tenantId);
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> {
                    log.error("Tenant not found: {}", tenantId);
                    return new RuntimeException("Tenant not found: " + tenantId);
                });
        return mapToDTO(tenant);
    }

    /**
     * Create a new tenant
     */
    public TenantDTO createTenant(String tenantId, String name, String dbUrl, String dbName,
                                   String dbUsername, String dbPassword) {
        log.info("Creating new tenant: {}", tenantId);

        Tenant tenant = Tenant.builder()
                .tenantId(tenantId)
                .name(name)
                .dbUrl(dbUrl)
                .dbName(dbName)
                .dbUsername(dbUsername)
                .dbPassword(dbPassword)
                .active(true)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created successfully: {}", tenantId);
        return mapToDTO(saved);
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
        tenant.setActive(false);
        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant deactivated: {}", tenantId);
        return mapToDTO(updated);
    }

    /**
     * Activate a tenant
     */
    public TenantDTO activateTenant(String tenantId) {
        log.info("Activating tenant: {}", tenantId);
        Tenant tenant = getTenantEntity(tenantId);
        tenant.setActive(true);
        Tenant updated = tenantRepository.save(tenant);
        log.info("Tenant activated: {}", tenantId);
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
                .active(tenant.isActive())
                .build();
    }
}

