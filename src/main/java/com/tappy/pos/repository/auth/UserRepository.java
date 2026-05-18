package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username — cross-tenant, use only where tenant isolation is not required
     * (e.g. JwtAuthenticationFilter which must locate a user from any tenant by JWT claim).
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by username scoped to the current tenant via current_tenant_id().
     * Use this for all login/auth flows to prevent cross-tenant credential reuse.
     * Relies on app.current_tenant being set (done by TenantRlsAspect on @Transactional entry).
     */
    @Query(value = "SELECT * FROM users WHERE username = :username " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND deleted <> true",
           nativeQuery = true)
    Optional<User> findByUsernameTenantScoped(@Param("username") String username);


    /**
     * Check if username exists within the current tenant.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE username = :username " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND deleted <> true",
           nativeQuery = true)
    boolean existsByUsernameTenantScoped(@Param("username") String username);

    /**
     * Check if email exists within the current tenant.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND deleted <> true",
           nativeQuery = true)
    boolean existsByEmailTenantScoped(@Param("email") String email);

    /**
     * Find user by ID scoped to the current tenant.
     */
    @Query(value = "SELECT * FROM users WHERE id = :id " +
           "AND tenant_id IS NOT DISTINCT FROM current_tenant_id() " +
           "AND deleted <> true",
           nativeQuery = true)
    Optional<User> findByIdTenantScoped(@Param("id") Long id);

    /**
     * Get all users with pagination, search, and filtering — scoped to the current tenant.
     */
    @Query(value = "SELECT * FROM users WHERE " +
            "tenant_id IS NOT DISTINCT FROM current_tenant_id() AND " +
            "(CAST(:search AS text) IS NULL OR LOWER(username) LIKE LOWER('%' || CAST(:search AS text) || '%') OR " +
            "LOWER(email) LIKE LOWER('%' || CAST(:search AS text) || '%') OR " +
            "LOWER(full_name) LIKE LOWER('%' || CAST(:search AS text) || '%')) AND " +
            "deleted <> true " +
            "ORDER BY id DESC",
           countQuery = "SELECT COUNT(*) FROM users WHERE " +
            "tenant_id IS NOT DISTINCT FROM current_tenant_id() AND " +
            "(CAST(:search AS text) IS NULL OR LOWER(username) LIKE LOWER('%' || CAST(:search AS text) || '%') OR " +
            "LOWER(email) LIKE LOWER('%' || CAST(:search AS text) || '%') OR " +
            "LOWER(full_name) LIKE LOWER('%' || CAST(:search AS text) || '%')) AND " +
            "deleted <> true",
           nativeQuery = true)
    Page<User> findAllWithSearch(
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Get all active users
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.deletedAt IS NULL ORDER BY u.id DESC")
    Page<User> findAllActiveUsers(Pageable pageable);

    /**
     * Find active user by username
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.active = true AND u.deletedAt IS NULL")
    Optional<User> findByUsernameActive(@Param("username") String username);

    /**
     * Find a pre-provisioned (registration-only) user with no tenant assigned yet.
     * Used during self-provision to promote the user to tenant scope instead of creating a second row.
     * Bypasses current_tenant_id() so it always finds the null-tenant registration record.
     */
    @Query(value = "SELECT * FROM users WHERE username = :username " +
                   "AND tenant_id IS NULL AND deleted <> true",
           nativeQuery = true)
    Optional<User> findByUsernameAndNullTenant(@Param("username") String username);

    /**
     * Global cross-tenant user lookup — bypasses RLS tenant filter.
     * Prefers tenant-scoped rows (non-null tenant_id) over pre-provision rows.
     * Use ONLY in auth flows where tenant context is not yet established (e.g. login without X-Tenant-ID).
     */
    @Query(value = "SELECT * FROM users WHERE username = :username AND deleted <> true " +
                   "ORDER BY tenant_id NULLS LAST LIMIT 1",
           nativeQuery = true)
    Optional<User> findByUsernameGlobal(@Param("username") String username);

    /**
     * Get all active usernames scoped to the current tenant.
     * Uses current_tenant_id() so this must be called within a @Transactional boundary
     * where TenantRlsAspect has already set app.current_tenant on the connection.
     */
    @Query(value = "SELECT username FROM users WHERE active = true AND deleted_at IS NULL " +
                   "AND tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    java.util.List<String> findAllActiveUsernames();

    /**
     * Get active usernames by role name — scoped to the current tenant.
     */
    @Query(value = "SELECT DISTINCT u.username FROM users u " +
                   "INNER JOIN user_roles ur ON u.id = ur.user_id " +
                   "INNER JOIN roles r ON ur.role_id = r.id " +
                   "WHERE r.name = :roleName AND u.active = true AND u.deleted_at IS NULL " +
                   "AND u.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    java.util.List<String> findUsernamesByRole(@Param("roleName") String roleName);

    /**
     * Get active usernames by any of the given role names — scoped to the current tenant.
     * DISTINCT prevents duplicates when a user holds multiple matching roles.
     */
    @Query(value = "SELECT DISTINCT u.username FROM users u " +
                   "INNER JOIN user_roles ur ON u.id = ur.user_id " +
                   "INNER JOIN roles r ON ur.role_id = r.id " +
                   "WHERE r.name IN :roleNames AND u.active = true AND u.deleted_at IS NULL " +
                   "AND u.tenant_id IS NOT DISTINCT FROM current_tenant_id()",
           nativeQuery = true)
    java.util.List<String> findUsernamesByRoleNames(@Param("roleNames") java.util.List<String> roleNames);

    /**
     * Returns the subset of the given usernames whose roles grant the specified feature.
     * Used to compute feature-based notification defaults for users without a pref row.
     */
    @Query(value = "SELECT DISTINCT u.username FROM users u " +
           "INNER JOIN user_roles ur ON u.id = ur.user_id " +
           "INNER JOIN roles r ON ur.role_id = r.id " +
           "INNER JOIN role_features rf ON r.id = rf.role_id " +
           "INNER JOIN features f ON rf.feature_id = f.id " +
           "WHERE u.username IN :usernames AND f.name = :featureName " +
           "AND u.active = TRUE AND u.deleted_at IS NULL " +
           "AND f.deleted = FALSE AND f.active = TRUE " +
           "AND r.tenant_id IS NOT DISTINCT FROM current_setting('app.current_tenant', true)",
           nativeQuery = true)
    java.util.Set<String> findUsernamesWithFeature(
            @Param("usernames") java.util.List<String> usernames,
            @Param("featureName") String featureName);
}

