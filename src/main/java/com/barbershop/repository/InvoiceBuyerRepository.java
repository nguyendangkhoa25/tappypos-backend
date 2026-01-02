package com.barbershop.repository;

import com.barbershop.model.entity.InvoiceBuyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceBuyerRepository extends JpaRepository<InvoiceBuyer, Long> {

    @Query("SELECT ib FROM InvoiceBuyer ib WHERE ib.customerId = :customerId")
    Optional<InvoiceBuyer> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT ib FROM InvoiceBuyer ib WHERE ib.buyerTaxCode = :taxCode")
    Optional<InvoiceBuyer> findByBuyerTaxCode(@Param("taxCode") String taxCode);
}

