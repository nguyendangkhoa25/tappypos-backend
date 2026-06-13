package com.tappy.pos.repository.table;

import com.tappy.pos.model.entity.table.ShopTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends JpaRepository<ShopTable, Long> {

    @Query("""
        SELECT t FROM ShopTable t
        WHERE t.tenantId = :tenantId AND t.deleted = false
        ORDER BY t.displayOrder ASC, t.tableNumber ASC
    """)
    List<ShopTable> findAllActive(@Param("tenantId") String tenantId);

    @Query("""
        SELECT t FROM ShopTable t
        WHERE t.tenantId = :tenantId AND t.tableNumber = :tableNumber AND t.deleted = false
    """)
    Optional<ShopTable> findByTenantIdAndTableNumber(
            @Param("tenantId") String tenantId,
            @Param("tableNumber") String tableNumber);

    @Query("""
        SELECT t FROM ShopTable t
        WHERE t.tenantId = :tenantId AND t.currentOrderId = :orderId AND t.deleted = false
    """)
    Optional<ShopTable> findByTenantIdAndCurrentOrderId(
            @Param("tenantId") String tenantId,
            @Param("orderId") Long orderId);

    // RLS scopes this to the current tenant; the token is the QR payload from the customer page.
    @Query("SELECT t FROM ShopTable t WHERE t.qrToken = :qrToken AND t.deleted = false")
    Optional<ShopTable> findByQrToken(@Param("qrToken") String qrToken);
}
