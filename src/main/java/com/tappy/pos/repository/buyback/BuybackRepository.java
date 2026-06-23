package com.tappy.pos.repository.buyback;

import com.tappy.pos.model.entity.buyback.BuybackEntity;
import com.tappy.pos.model.enums.BuybackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface BuybackRepository extends JpaRepository<BuybackEntity, Long> {

    @Query("SELECT b FROM BuybackEntity b WHERE (b.visible IS NULL OR b.visible = true) " +
            "AND (:status IS NULL OR b.status = :status) ORDER BY b.purchaseDate DESC")
    Page<BuybackEntity> findAllVisible(@Param("status") BuybackStatus status, Pageable pageable);

    /** Total margin (resale − acquisition) for SOLD buybacks sold in the period. */
    @Query("SELECT COALESCE(SUM(b.resalePrice - b.acquisitionPrice), 0) FROM BuybackEntity b " +
            "WHERE b.status = 'SOLD' AND (b.visible IS NULL OR b.visible = true) " +
            "AND b.soldDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumMarginBySoldDateBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    /** Count + tied-up cash of unsold (PURCHASED/LISTED) buybacks — "tồn đồ cũ". */
    @Query("SELECT COUNT(b.buybackId) FROM BuybackEntity b " +
            "WHERE b.status IN ('PURCHASED','LISTED') AND (b.visible IS NULL OR b.visible = true)")
    long countUnsold();

    @Query("SELECT COALESCE(SUM(b.acquisitionPrice), 0) FROM BuybackEntity b " +
            "WHERE b.status IN ('PURCHASED','LISTED') AND (b.visible IS NULL OR b.visible = true)")
    BigDecimal sumUnsoldAcquisition();
}
