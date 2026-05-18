package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RoleFeatureRepository — data access for role-feature mappings.
 *
 * features has no tenant_id — it is global master data.
 * roles is tenant-scoped; IS NOT DISTINCT FROM current_tenant_id() gives null-safe matching.
 */
@Repository
public interface RoleFeatureRepository extends JpaRepository<Feature, Long> {

    @Query(value = "SELECT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT DISTINCT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query(value = "SELECT DISTINCT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query(value = "SELECT DISTINCT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id = :tenantId " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleNamesAndTenantId(@Param("roleNames") List<String> roleNames, @Param("tenantId") String tenantId);

    @Query(value = "SELECT COUNT(*) > 0 FROM role_features rf " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "INNER JOIN features f ON rf.feature_id = f.id " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    boolean hasRoleAccessToFeature(@Param("roleName") String roleName, @Param("featureName") String featureName);

    @Query(value = "SELECT f.* FROM features f " +
           "WHERE f.deleted = FALSE AND f.active = TRUE " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findAllActiveFeatures();

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO role_features (role_id, feature_id) " +
           "SELECT r.id, f.id " +
           "FROM roles r, features f " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "ON CONFLICT (role_id, feature_id) DO NOTHING",
           nativeQuery = true)
    void assignFeatureToRole(@Param("roleName") String roleName, @Param("featureName") String featureName);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM role_features " +
           "USING roles r, features f " +
           "WHERE role_features.role_id = r.id AND role_features.feature_id = f.id " +
           "AND r.name = :roleName AND f.name = :featureName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    void removeFeatureFromRole(@Param("roleName") String roleName, @Param("featureName") String featureName);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM role_features " +
           "USING roles r " +
           "WHERE role_features.role_id = r.id " +
           "AND r.name = :roleName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    void removeAllFeaturesFromRole(@Param("roleName") String roleName);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM role_features " +
           "USING roles r, features f " +
           "WHERE role_features.role_id = r.id AND role_features.feature_id = f.id " +
           "AND f.name = :featureName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    void removeFeatureFromAllRoles(@Param("featureName") String featureName);
}
