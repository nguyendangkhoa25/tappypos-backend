package com.tappy.pos.repository.tax;

import com.tappy.pos.model.entity.finance.TaxRateCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateCatalogRepository extends JpaRepository<TaxRateCatalog, Long> {

    List<TaxRateCatalog> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

    Optional<TaxRateCatalog> findByCodeAndDeletedFalse(String code);
}
