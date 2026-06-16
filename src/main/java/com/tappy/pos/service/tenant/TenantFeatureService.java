package com.tappy.pos.service.tenant;

import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.auth.RoleFeatureRepository;
import com.tappy.pos.repository.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.tappy.pos.model.entity.auth.Feature;
import com.tappy.pos.model.entity.auth.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TenantFeatureService - Manages feature access based on tenant and role
 * <p>
 * Feature Access Logic:
 * 1. Get features assigned to the TENANT from master DB
 * 2. Get features assigned to the ROLE from master DB
 * 3. Return INTERSECTION of tenant features and role features
 * <p>
 * This ensures:
 * - Tenants can only use features they are subscribed to
 * - Users can only use features their role has access to
 * - Users can only use features their tenant has enabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantFeatureService {

    private final RoleFeatureRepository roleFeatureRepository;
    private final RoleRepository roleRepository;
    private final TenantContext tenantContext;

    /**
     * Get accessible features for a user based on tenant and role
     *
     * @param roleNames list of role names the user has
     * @return list of feature names the user can access (intersection of tenant and role features)
     */
    public List<String> getAccessibleFeaturesByRoleAndTenant(List<String> roleNames) {
        log.info("Fetching accessible features for roles: {} with tenant context", roleNames);

        // Get current tenant from context
        Tenant currentTenant = tenantContext.getCurrentTenant();
        if (currentTenant == null) {
            log.warn("No tenant context found, returning all role features");
            return roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames);
        }

        // Step 1: Get features assigned to the tenant
        Set<String> tenantFeatures = getTenantFeatures(currentTenant);
        log.info("Tenant {} has {} features assigned", currentTenant.getTenantId(), tenantFeatures.size());

//        // SHOP_OWNER always gets all tenant features — no role_features intersection needed.
//        if (roleNames.contains(RoleEnum.SHOP_OWNER.getCode())) {
//            log.info("SHOP_OWNER role detected — granting all {} tenant features", tenantFeatures.size());
//            return new ArrayList<>(tenantFeatures);
//        }

        // Step 2: Get features assigned to the roles — use explicit tenantId to avoid
        // relying on app.current_tenant session var, which may be NULL when login is
        // called without X-Tenant-ID (tenant auto-detection path).
        List<String> roleFeatures = roleFeatureRepository.findActiveFeatureNamesByRoleNamesAndTenantId(roleNames, currentTenant.getTenantId());
        log.info("Roles {} have {} features assigned", roleNames, roleFeatures.size());

        // Step 3: Get intersection - features in both tenant and role
        List<String> accessibleFeatures = roleFeatures.stream()
                .filter(tenantFeatures::contains)
                .collect(Collectors.toList());

        log.info("User with roles {} and tenant {} has access to {} features",
                roleNames, currentTenant.getTenantId(), accessibleFeatures.size());
        log.debug("Accessible features: {}", accessibleFeatures);
        return accessibleFeatures;
    }

    /**
     * Get accessible features for a user.
     * If the user has explicit per-user features set (user_features table), those are used
     * instead of role features — intersected with tenant features.
     * Falls back to role-based resolution when no user features are configured.
     *
     * @param user      the user (with userFeatures loaded)
     * @param roleNames list of role names the user has (used as fallback)
     * @return list of feature names the user can access
     */
    public List<String> getAccessibleFeaturesByUserAndTenant(User user, List<String> roleNames) {
        if (user.getUserFeatures() != null && !user.getUserFeatures().isEmpty()) {
            log.info("User {} has {} user-specific features — using override instead of role features",
                    user.getUsername(), user.getUserFeatures().size());
            Set<String> userFeatureNames = user.getUserFeatures().stream()
                    .map(Feature::getName)
                    .collect(Collectors.toSet());

            Tenant currentTenant = tenantContext.getCurrentTenant();
            if (currentTenant == null) {
                return new ArrayList<>(userFeatureNames);
            }
            Set<String> tenantFeatures = getTenantFeatures(currentTenant);
            return userFeatureNames.stream()
                    .filter(tenantFeatures::contains)
                    .collect(Collectors.toList());
        }
        // No override — fall back to role-based resolution
        return getAccessibleFeaturesByRoleAndTenant(roleNames);
    }

    /**
     * Get all features assigned to the current tenant
     *
     * @return set of feature names assigned to the tenant
     */
    public Set<String> getTenantAssignedFeatures() {
        Tenant currentTenant = tenantContext.getCurrentTenant();

        if (currentTenant == null) {
            log.warn("No tenant context found, returning empty feature set");
            return new HashSet<>();
        }

        return getTenantFeatures(currentTenant);
    }

    /**
     * Check if a user has access to a specific feature
     *
     * @param roleNames   list of role names
     * @param featureName the feature to check
     * @return true if user has access to the feature
     */
    public boolean hasAccessToFeature(List<String> roleNames, String featureName) {
        List<String> accessibleFeatures = getAccessibleFeaturesByRoleAndTenant(roleNames);
        boolean hasAccess = accessibleFeatures.contains(featureName);

        log.debug("User with roles {} has access to feature {}: {}", roleNames, featureName, hasAccess);
        return hasAccess;
    }

    /**
     * Check if a user has access to a specific feature (single role)
     */
    public boolean hasAccessToFeature(String roleName, String featureName) {
        return hasAccessToFeature(List.of(roleName), featureName);
    }

    /**
     * Parse tenant features from comma-separated string
     * Format: "DASHBOARD,ORDER,PRODUCT,PROMOTION" etc.
     *
     * @param tenant the tenant entity
     * @return set of feature names assigned to the tenant
     */
    private Set<String> getTenantFeatures(Tenant tenant) {
        if (tenant == null || tenant.getFeatures() == null || tenant.getFeatures().trim().isEmpty()) {
            log.debug("Tenant {} has no features assigned", tenant != null ? tenant.getTenantId() : "null");
            return new HashSet<>();
        }

        // Parse comma-separated features
        Set<String> features = Arrays.stream(tenant.getFeatures().split(","))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toSet());

        log.debug("Parsed {} features for tenant {}: {}", features.size(), tenant.getTenantId(), features);
        return features;
    }

    /**
     * Returns the role names for the SHOP_OWNER role in the given tenant.
     * Falls back to ["SHOP_OWNER"] if the role row is not yet persisted
     * (e.g. called immediately after provisioning before the transaction commits).
     *
     * @param tenantId the tenant whose role table is queried
     * @return a single-element list with the resolved role name
     */
    public List<String> getShopOwnerRoleNames(String tenantId) {
        return roleRepository.findByNameAndTenantId("SHOP_OWNER", tenantId)
                .map(r -> List.of(r.getName()))
                .orElse(List.of("SHOP_OWNER"));
    }

    /**
     * Validate that all requested features are available for the tenant
     * Useful for checking if a subscription upgrade/downgrade would allow certain features
     */
    public boolean validateTenantHasFeatures(List<String> requestedFeatures) {
        Set<String> tenantFeatures = getTenantAssignedFeatures();
        return requestedFeatures.stream()
                .allMatch(tenantFeatures::contains);
    }
}

