package com.tappy.pos.repository.stocktake;

import com.tappy.pos.model.entity.stocktake.StocktakeSessionEntity;
import com.tappy.pos.model.enums.StocktakeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Tenant-scoped via PostgreSQL RLS (app.current_tenant) — no explicit tenant_id filter needed.
 */
@Repository
public interface StocktakeSessionRepository extends JpaRepository<StocktakeSessionEntity, Long> {

    Optional<StocktakeSessionEntity> findByIdAndDeletedFalse(Long id);

    /** The current open session, if any (used to resume / prevent multiple active sessions). */
    Optional<StocktakeSessionEntity> findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus status);

    Page<StocktakeSessionEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<StocktakeSessionEntity> findByStatusAndDeletedFalseOrderByCreatedAtDesc(StocktakeStatus status, Pageable pageable);
}
