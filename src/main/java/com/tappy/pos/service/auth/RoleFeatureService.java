package com.tappy.pos.service.auth;

import com.tappy.pos.model.dto.auth.PermissionsMatrixDTO;
import com.tappy.pos.model.entity.auth.Feature;
import com.tappy.pos.model.entity.auth.Role;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.repository.auth.RoleFeatureRepository;
import com.tappy.pos.repository.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RoleFeatureService - Role-based access control business logic
 * Handles fetching and managing role-feature mappings
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoleFeatureService {

    private final RoleFeatureRepository roleFeatureRepository;
    private final RoleRepository roleRepository;

    /**
     * Get all active features accessible by a specific role
     *
     * @param roleName the name of the role (e.g., 'SHOP_OWNER', 'MANAGER')
     * @return list of Feature objects accessible by the role
     */
    public List<Feature> getActiveFeaturesByRoleName(String roleName) {
        log.info("Fetching active features for role: {}", roleName);
        List<Feature> features = roleFeatureRepository.findActiveFeaturesByRoleName(roleName);
        log.info("Found {} active features for role: {}", features.size(), roleName);
        return features;
    }

    /**
     * Get all active feature names (codes) accessible by a specific role
     * Returns only the feature names/codes like 'DASHBOARD', 'ORDER', etc.
     *
     * @param roleName the name of the role
     * @return list of feature names (codes)
     */
    public List<String> getActiveFeatureNamesByRoleName(String roleName) {
        log.info("Fetching active feature names for role: {}", roleName);
        List<String> featureNames = roleFeatureRepository.findActiveFeatureNamesByRoleName(roleName);
        log.info("Found {} active feature names for role: {}", featureNames.size(), roleName);
        return featureNames;
    }

    /**
     * Get all active features accessible by any of the given roles
     * Used for users with multiple roles
     *
     * @param roleNames list of role names
     * @return list of Feature objects accessible by any of the roles
     */
    public List<Feature> getActiveFeaturesByRoleNames(List<String> roleNames) {
        log.info("Fetching active features for roles: {}", roleNames);
        List<Feature> features = roleFeatureRepository.findActiveFeaturesByRoleNames(roleNames);
        log.info("Found {} active features for roles: {}", features.size(), roleNames);
        return features;
    }

    /**
     * Get all active feature names accessible by any of the given roles
     * Used for users with multiple roles
     *
     * @param roleNames list of role names
     * @return list of feature names (codes)
     */
    public List<String> getActiveFeatureNamesByRoleNames(List<String> roleNames) {
        log.info("Fetching active feature names for roles: {}", roleNames);
        List<String> featureNames = roleFeatureRepository.findActiveFeatureNamesByRoleNames(roleNames);
        log.info("Found {} active feature names for roles: {}", featureNames.size(), roleNames);
        return featureNames;
    }

    /**
     * Check if a role has access to a specific feature
     *
     * @param roleName the name of the role
     * @param featureName the name of the feature
     * @return true if role has access to the feature, false otherwise
     */
    public boolean hasRoleAccessToFeature(String roleName, String featureName) {
        boolean hasAccess = roleFeatureRepository.hasRoleAccessToFeature(roleName, featureName);
        log.debug("Role {} access to feature {}: {}", roleName, featureName, hasAccess);
        return hasAccess;
    }

    /**
     * Get all active features in the system
     */
    public List<Feature> getAllActiveFeatures() {
        log.info("Fetching all active features");
        List<Feature> features = roleFeatureRepository.findAllActiveFeatures();
        log.info("Found {} active features total", features.size());
        return features;
    }

    /**
     * Returns the full permissions matrix: all roles × all features with assigned flags
     */
    @Transactional(readOnly = true)
    public PermissionsMatrixDTO getPermissionsMatrix() {
        List<Feature> allFeatures = roleFeatureRepository.findAllActiveFeatures();

        List<PermissionsMatrixDTO.FeatureInfo> featureInfos = allFeatures.stream()
                .map(f -> PermissionsMatrixDTO.FeatureInfo.builder()
                        .name(f.getName())
                        .displayName(f.getDisplayName())
                        .description(f.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<String> allFeatureNames = allFeatures.stream()
                .map(Feature::getName)
                .collect(Collectors.toList());

        // Master-only roles must never appear in shop user management
        Set<String> masterOnlyRoles = Set.of(
                RoleEnum.MASTER_TENANT.getCode(), RoleEnum.AGENT.getCode());

        List<PermissionsMatrixDTO.RolePermissions> rolePerms = roleRepository.findAll().stream()
                .filter(r -> !masterOnlyRoles.contains(r.getName()))
                .map(role -> {
                    List<String> assigned = RoleEnum.SHOP_OWNER.getCode().equals(role.getName())
                            ? allFeatureNames
                            : roleFeatureRepository.findActiveFeatureNamesByRoleName(role.getName());
                    return PermissionsMatrixDTO.RolePermissions.builder()
                            .roleName(role.getName())
                            .description(role.getDescription())
                            .featureNames(assigned)
                            .build();
                })
                .collect(Collectors.toList());

        return PermissionsMatrixDTO.builder()
                .features(featureInfos)
                .roles(rolePerms)
                .build();
    }

    /**
     * Replace all features for a role with the provided list
     */
    public void setRoleFeatures(String roleName, List<String> featureNames) {
        log.info("Setting features for role {}: {}", roleName, featureNames);
        roleFeatureRepository.removeAllFeaturesFromRole(roleName);
        if (featureNames != null) {
            for (String featureName : featureNames) {
                roleFeatureRepository.assignFeatureToRole(roleName, featureName);
            }
        }
    }

    /**
     * Syncs role_features when master admin changes a tenant's feature set.
     * Added features → assigned to SHOP_OWNER only (other roles stay as configured).
     * Removed features → deleted from ALL roles so nobody retains a revoked feature.
     * Must be called with the target tenant's RLS context already set.
     */
    public void syncTenantFeatureChanges(List<String> added, List<String> removed) {
        for (String feature : added) {
            roleFeatureRepository.assignFeatureToRole("SHOP_OWNER", feature);
            log.info("Assigned feature {} to SHOP_OWNER", feature);
        }
        for (String feature : removed) {
            roleFeatureRepository.removeFeatureFromAllRoles(feature);
            log.info("Removed feature {} from all roles", feature);
        }
    }
}

