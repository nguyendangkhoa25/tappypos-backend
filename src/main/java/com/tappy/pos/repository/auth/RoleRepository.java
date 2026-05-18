package com.tappy.pos.repository.auth;

import com.tappy.pos.model.entity.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

    // Provisioning helpers — explicit tenant_id avoids relying on Hibernate filter state
    Optional<Role> findByNameAndTenantId(String name, String tenantId);
    boolean existsByNameAndTenantId(String name, String tenantId);
}

