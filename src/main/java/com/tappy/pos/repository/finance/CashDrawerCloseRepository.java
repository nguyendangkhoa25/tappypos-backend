package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.CashDrawerClose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Queries run inside the RLS-scoped transaction, so they are automatically limited to the
 * current tenant (UNIQUE(tenant_id, business_date) then yields at most one row per date).
 */
public interface CashDrawerCloseRepository extends JpaRepository<CashDrawerClose, Long> {

    /** The (existing) close for a given business day, if already reconciled. */
    Optional<CashDrawerClose> findByBusinessDateAndDeletedFalse(LocalDate businessDate);

    /** Most recent prior close — its counted amount is the opening float carried into the next day. */
    Optional<CashDrawerClose> findTopByBusinessDateLessThanAndDeletedFalseOrderByBusinessDateDesc(LocalDate businessDate);
}
