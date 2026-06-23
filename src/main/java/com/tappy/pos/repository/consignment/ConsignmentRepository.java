package com.tappy.pos.repository.consignment;

import com.tappy.pos.model.entity.consignment.Consignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Native queries here bypass the Hibernate @Filter on TenantAwareEntity, so tenant
 * isolation is enforced explicitly via AND tenant_id = :tenantId in every WHERE clause.
 * The :createdBy bind narrows the list to the caller's own placements when they lack
 * CONSIGNMENT_VIEW_ALL (pass null to see all).
 */
@Repository
public interface ConsignmentRepository extends JpaRepository<Consignment, Long> {

    @Query(value = "SELECT * FROM consignment WHERE deleted = FALSE " +
            "AND tenant_id = :tenantId " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:createdBy IS NULL OR created_by = :createdBy) " +
            "ORDER BY placement_date DESC, id DESC",
            countQuery = "SELECT COUNT(*) FROM consignment WHERE deleted = FALSE " +
            "AND tenant_id = :tenantId " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:createdBy IS NULL OR created_by = :createdBy)",
            nativeQuery = true)
    Page<Consignment> search(
            @Param("tenantId") String tenantId,
            @Param("status") String status,
            @Param("createdBy") String createdBy,
            Pageable pageable);
}
