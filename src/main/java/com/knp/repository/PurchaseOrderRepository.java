package com.knp.repository;

import com.knp.model.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT p FROM PurchaseOrder p WHERE p.deleted = false ORDER BY p.createdAt DESC")
    Page<PurchaseOrder> findAllActive(Pageable pageable);

    @Query("""
        SELECT p FROM PurchaseOrder p
        WHERE p.deleted = false AND p.status = :status
        ORDER BY p.createdAt DESC
        """)
    Page<PurchaseOrder> findByStatus(@Param("status") PurchaseOrder.PoStatus status, Pageable pageable);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.deleted = false AND p.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    Page<PurchaseOrder> findByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.poNumber, 5) AS int)), 0) FROM PurchaseOrder p WHERE p.poNumber LIKE 'PO-%'")
    Integer findMaxPoSequence();
}
