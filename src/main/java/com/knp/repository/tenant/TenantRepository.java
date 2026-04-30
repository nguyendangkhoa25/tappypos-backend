package com.knp.repository.tenant;

import com.knp.model.entity.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantId(String tenantId);

    List<Tenant> findAllByActiveTrue();

    List<Tenant> findAllByVendorId(Long vendorId);

    @Query("SELECT t FROM Tenant t WHERE t.active = true AND t.expirationDate = :targetDate")
    List<Tenant> findActiveTenantsExpiringOn(@Param("targetDate") LocalDate targetDate);
}