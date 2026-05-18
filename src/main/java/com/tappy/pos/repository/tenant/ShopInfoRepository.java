package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.ShopInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopInfoRepository extends JpaRepository<ShopInfo, Long> {

    /**
     * Find shop info by ID and check if it's not deleted
     * @param id Shop ID
     * @return Optional of ShopInfo
     */
    Optional<ShopInfo> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Find first active shop info (there should be only one per tenant)
     * @return Optional of first active ShopInfo
     */
    Optional<ShopInfo> findFirstByDeletedAtIsNullOrderByIdAsc();

    // Provisioning helper — explicit tenant_id avoids relying on Hibernate filter state
    Optional<ShopInfo> findFirstByTenantIdAndDeletedAtIsNullOrderByIdAsc(String tenantId);
}

