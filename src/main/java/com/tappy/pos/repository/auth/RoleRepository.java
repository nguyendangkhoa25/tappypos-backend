package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

    // Provisioning helpers — explicit tenant_id avoids relying on Hibernate filter state
    Optional<Role> findByNameAndTenantId(String name, String tenantId);
    boolean existsByNameAndTenantId(String name, String tenantId);

    List<Role> findByTenantId(String tenantId);

    /**
     * Remove all user-role assignments for users that are being unlinked from a tenant.
     * Must be called BEFORE unlinkAllFromTenant so the join table is clean.
     */
    @Modifying
    @Query(value = "DELETE FROM user_roles ur " +
                   "USING roles r " +
                   "WHERE ur.role_id = r.id AND r.tenant_id = :tenantId",
           nativeQuery = true)
    int deleteUserRoleAssignmentsByTenantId(@Param("tenantId") String tenantId);
}

