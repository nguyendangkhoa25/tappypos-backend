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
 * RoleFeatureRepository — data access for role-feature mappings.
 *
 * Uses IS NOT DISTINCT FROM current_tenant_id() for null-safe tenant scoping.
 * current_tenant_id() returns NULL in master context, matching master rows (tenant_id IS NULL).
 * current_setting('app.current_tenant', true) returns '' in master context — plain = would fail.
 */
@Repository
public interface RoleFeatureRepository extends JpaRepository<Feature, Long> {

    @Query(value = "SELECT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name = :roleName AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT DISTINCT f.* FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findActiveFeaturesByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query(value = "SELECT DISTINCT f.name FROM features f " +
           "INNER JOIN role_features rf ON f.id = rf.feature_id " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "WHERE r.name IN :roleNames AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<String> findActiveFeatureNamesByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query(value = "SELECT COUNT(*) > 0 FROM role_features rf " +
           "INNER JOIN roles r ON rf.role_id = r.id " +
           "INNER JOIN features f ON rf.feature_id = f.id " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL)",
           nativeQuery = true)
    boolean hasRoleAccessToFeature(@Param("roleName") String roleName, @Param("featureName") String featureName);

    @Query(value = "SELECT DISTINCT f.* FROM features f " +
           "WHERE f.deleted = FALSE AND f.active = TRUE " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ORDER BY f.name ASC", nativeQuery = true)
    List<Feature> findAllActiveFeatures();

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO role_features (tenant_id, role_id, feature_id) " +
           "SELECT current_tenant_id(), r.id, f.id " +
           "FROM roles r, features f " +
           "WHERE r.name = :roleName AND f.name = :featureName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL) " +
           "ON CONFLICT (role_id, feature_id) DO NOTHING",
           nativeQuery = true)
    void assignFeatureToRole(@Param("roleName") String roleName, @Param("featureName") String featureName);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM role_features " +
           "USING roles r, features f " +
           "WHERE role_features.role_id = r.id AND role_features.feature_id = f.id " +
           "AND r.name = :roleName AND f.name = :featureName " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND (f.tenant_id IS NOT DISTINCT FROM current_tenant_id() OR f.tenant_id IS NULL)",
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
}
