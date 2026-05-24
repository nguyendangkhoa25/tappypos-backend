package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.ShopDeletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopDeletionLogRepository extends JpaRepository<ShopDeletionLog, Long> {
    List<ShopDeletionLog> findByTenantIdOrderByDeletedAtDesc(String tenantId);
}
