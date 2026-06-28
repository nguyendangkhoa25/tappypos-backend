package com.tappy.pos.repository.tax;

import com.tappy.pos.model.entity.finance.TaxDeclaration;
import com.tappy.pos.model.enums.TaxPeriodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Tenant-scoped via PostgreSQL RLS (app.current_tenant). No explicit tenant filter needed.
 */
@Repository
public interface TaxDeclarationRepository extends JpaRepository<TaxDeclaration, Long> {

    Optional<TaxDeclaration> findByIdAndDeletedFalse(Long id);

    Page<TaxDeclaration> findByDeletedFalseOrderByPeriodYearDescPeriodNumberDesc(Pageable pageable);

    Page<TaxDeclaration> findByPeriodYearAndDeletedFalseOrderByPeriodNumberDesc(Integer periodYear, Pageable pageable);

    Optional<TaxDeclaration> findByPeriodTypeAndPeriodYearAndPeriodNumberAndDeletedFalse(
            TaxPeriodType periodType, Integer periodYear, Integer periodNumber);
}
