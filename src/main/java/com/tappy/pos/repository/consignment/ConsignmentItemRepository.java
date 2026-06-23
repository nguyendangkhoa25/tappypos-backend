package com.tappy.pos.repository.consignment;

import com.tappy.pos.model.entity.consignment.ConsignmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsignmentItemRepository extends JpaRepository<ConsignmentItem, Long> {

    /**
     * Settle-by-sales: units sold of each consigned product within the period, counted
     * from COMPLETED orders (passive — no checkout coupling). Returns rows of
     * [product_id (BIGINT), quantity_sold (BIGINT)]. Tenant-scoped explicitly.
     */
    @Query(value = "SELECT oi.product_id, COALESCE(SUM(oi.quantity), 0) " +
            "FROM order_items oi " +
            "JOIN orders o ON o.id = oi.order_id AND o.tenant_id = oi.tenant_id " +
            "WHERE oi.tenant_id = :tenantId " +
            "AND oi.deleted = FALSE " +
            "AND oi.product_id IN (:productIds) " +
            "AND o.status = 'COMPLETED' " +
            "AND COALESCE(o.completed_at, o.created_at) >= CAST(:from AS timestamp) " +
            "AND COALESCE(o.completed_at, o.created_at) < (CAST(:to AS timestamp) + INTERVAL '1 day') " +
            "GROUP BY oi.product_id",
            nativeQuery = true)
    List<Object[]> sumSoldByProductIds(
            @Param("tenantId") String tenantId,
            @Param("productIds") List<Long> productIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
