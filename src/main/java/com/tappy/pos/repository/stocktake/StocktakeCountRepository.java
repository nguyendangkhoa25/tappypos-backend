package com.tappy.pos.repository.stocktake;

import com.tappy.pos.model.entity.stocktake.StocktakeCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant-scoped via PostgreSQL RLS (app.current_tenant) — no explicit tenant_id filter needed.
 */
@Repository
public interface StocktakeCountRepository extends JpaRepository<StocktakeCountEntity, Long> {

    List<StocktakeCountEntity> findBySessionIdAndDeletedFalseOrderByCountedAtDesc(Long sessionId);

    Optional<StocktakeCountEntity> findBySessionIdAndProductIdAndDeletedFalse(Long sessionId, Long productId);

    Optional<StocktakeCountEntity> findByIdAndDeletedFalse(Long id);

    long countBySessionIdAndDeletedFalse(Long sessionId);

    /** Counts with a non-zero discrepancy (counted ≠ system). */
    @Query("SELECT c FROM StocktakeCountEntity c WHERE c.deleted = false AND c.sessionId = :sessionId AND c.difference <> 0 ORDER BY c.countedAt DESC")
    List<StocktakeCountEntity> findDiscrepancies(@Param("sessionId") Long sessionId);

    /** Product ids already counted in this session — used to compute the "uncounted" list. */
    @Query("SELECT c.productId FROM StocktakeCountEntity c WHERE c.deleted = false AND c.sessionId = :sessionId")
    List<Long> findCountedProductIds(@Param("sessionId") Long sessionId);
}
