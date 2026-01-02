package com.barbershop.repository;

import com.barbershop.model.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId ORDER BY ii.lineNumber ASC")
    List<InvoiceItem> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId AND ii.orderItemId = :orderItemId")
    InvoiceItem findByInvoiceIdAndOrderItemId(@Param("invoiceId") Long invoiceId, @Param("orderItemId") Long orderItemId);
}

