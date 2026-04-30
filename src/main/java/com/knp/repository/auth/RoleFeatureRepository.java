package com.knp.repository.auth;

import com.knp.model.entity.auth.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RoleFeatureRepository - Data access for role-feature mappings
 * Queries the role_features table directly without needing RoleFeature entity
 */
@Repository
public interface RoleFeatureRepository extends JpaRepository<Feature, Long> {

    /**
     * Find all features accessible by a specific role
     * Uses direct SQL to query role_features junction table
     *
     * @param roleName the name of the role (e.g., 'SHOP_OWNER', 'MANAGER')
     * @return list of features accessible by the role
     */
    @Query(value = "SELECT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = 0 AND f.active = 1 " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleName(@Param("roleName") String roleName);

    /**
     * Get feature names accessible by a specific role
     *
     * @param roleName the name of the role
     * @return list of feature names (codes)
     */
    @Query(value = "SELECT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = 0 AND f.active = 1 " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleName(@Param("roleName") String roleName);

    /**
     * Get all features accessible by any of the given role names
     *
     * @param roleNames list of role names
     * @return list of features accessible by any of the roles
     */
    @Query(value = "SELECT DISTINCT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = 0 AND f.active = 1 " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * Get feature names accessible by any of the given role names
     *
     * @param roleNames list of role names
     * @return list of feature names (codes)
     */
    @Query(value = "SELECT DISTINCT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = 0 AND f.active = 1 " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * Check if a role has access to a specific feature
     *
     * @param roleName the name of the role
     * @param featureName the name of the feature
     * @return true if role has access, false otherwise
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM role_features rf " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "INNER JOIN features f ON rf.feature_id = f.id " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "AND f.deleted = 0 AND f.active = 1", nativeQuery = true)
    boolean hasRoleAccessToFeature(@Param("roleName") String roleName, @Param("featureName") String featureName);

    /**
     * Get all active features in the system
     */
    @Query(value = "SELECT DISTINCT f.* FROM features f " +
           "WHERE f.deleted = 0 AND f.active = 1 " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findAllActiveFeatures();

    /**
     * Assign a feature to a role (idempotent — ignores duplicates)
     */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO role_features (role_id, feature_id) " +
           "SELECT r.id, f.id FROM roles r, features f " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "ON DUPLICATE KEY UPDATE role_id = role_id",
           nativeQuery = true)
    void assignFeatureToRole(@Param("roleName") String roleName, @Param("featureName") String featureName);

    /**
     * Remove a single feature from a role
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE rf FROM role_features rf " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "INNER JOIN features f ON rf.feature_id = f.id " +
           "WHERE r.name = :roleName AND f.name = :featureName",
           nativeQuery = true)
    void removeFeatureFromRole(@Param("roleName") String roleName, @Param("featureName") String featureName);

    /**
     * Remove all features from a role
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE rf FROM role_features rf " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName",
           nativeQuery = true)
    void removeAllFeaturesFromRole(@Param("roleName") String roleName);
}

