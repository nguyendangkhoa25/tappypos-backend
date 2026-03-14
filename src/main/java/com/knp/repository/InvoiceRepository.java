package com.knp.repository;

import com.knp.model.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.deleted = false ORDER BY i.createdAt DESC")
    Page<Invoice> findAllActive(Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.deleted = false AND i.status = :status ORDER BY i.createdAt DESC")
    Page<Invoice> findAllActiveByStatus(@Param("status") Invoice.InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i JOIN i.orders o WHERE i.deleted = false AND o.id = :orderId")
    Optional<Invoice> findByOrderId(@Param("orderId") Long orderId);

    @Query("""
            SELECT i FROM Invoice i LEFT JOIN i.orders o
            WHERE i.deleted = false
              AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY i.createdAt DESC
            """)
    Page<Invoice> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByInvoiceNumber(String invoiceNumber);

    long countByDeletedFalse();
}
