package com.barbershop.repository;

import com.barbershop.model.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL")
    Page<Invoice> findAllActive(Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.id = :id")
    Optional<Invoice> findByIdActive(Long id);

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.order.id = :orderId")
    Optional<Invoice> findByOrderId(Long orderId);

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.invoiceNumber = :invoiceNumber")
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.status = :status")
    Page<Invoice> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.externalInvoiceId IS NOT NULL")
    List<Invoice> findAllSyncedWithExternal();

    @Query("SELECT i FROM Invoice i WHERE i.deletedAt IS NULL AND i.status = 'SYNCED_WITH_EXTERNAL'")
    Page<Invoice> findAllSyncedWithExternal(Pageable pageable);
}

