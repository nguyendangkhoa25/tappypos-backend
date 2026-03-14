package com.knp.service;

import com.knp.model.entity.Feature;
import com.knp.repository.RoleFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RoleFeatureService - Role-based access control business logic
 * Handles fetching and managing role-feature mappings
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoleFeatureService {

    private final RoleFeatureRepository roleFeatureRepository;

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
     *
     * @return list of all active Feature objects
     */
    public List<Feature> getAllActiveFeatures() {
        log.info("Fetching all active features");
        List<Feature> features = roleFeatureRepository.findAllActiveFeatures();
        log.info("Found {} active features total", features.size());
        return features;
    }
}

