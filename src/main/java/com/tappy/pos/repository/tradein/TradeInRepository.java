package com.tappy.pos.repository.tradein;

import com.tappy.pos.model.entity.tradein.TradeInEntity;
import com.tappy.pos.model.enums.TradeInStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TradeInRepository extends JpaRepository<TradeInEntity, Long> {

    Optional<TradeInEntity> findByIdAndDeletedFalse(Long id);

    @Query("SELECT t FROM TradeInEntity t WHERE t.deleted = false " +
            "AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<TradeInEntity> findAllActive(@Param("status") TradeInStatus status, Pageable pageable);

    @Query("SELECT t FROM TradeInEntity t WHERE t.deleted = false AND t.createdBy = :createdBy " +
            "AND (:status IS NULL OR t.status = :status) ORDER BY t.createdAt DESC")
    Page<TradeInEntity> findAllActiveByCreatedBy(@Param("status") TradeInStatus status,
                                                 @Param("createdBy") String createdBy, Pageable pageable);

    /** Total cash paid out for trade-ins completed in the period (for the end-of-day report). */
    @Query("SELECT COALESCE(SUM(t.tradeValue), 0) FROM TradeInEntity t " +
            "WHERE t.deleted = false AND t.status = 'COMPLETED' AND t.tenantId = :tenantId " +
            "AND t.createdAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumTradeValueBetween(@Param("tenantId") String tenantId,
                                    @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
