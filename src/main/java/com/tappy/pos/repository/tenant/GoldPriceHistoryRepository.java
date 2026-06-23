package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.GoldPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GoldPriceHistoryRepository extends JpaRepository<GoldPriceHistory, Long> {

    /** Snapshots since {@code from}, oldest first — for the shop's own gold-price chart. */
    @Query("SELECT h FROM GoldPriceHistory h WHERE h.deleted = false AND h.recordedAt >= :from " +
           "ORDER BY h.recordedAt ASC, h.code ASC")
    List<GoldPriceHistory> findSince(@Param("from") LocalDateTime from);
}
