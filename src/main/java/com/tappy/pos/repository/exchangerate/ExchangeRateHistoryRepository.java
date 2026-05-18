package com.tappy.pos.repository.exchangerate;

import com.tappy.pos.model.entity.exchangerate.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    @Query("""
            SELECT h FROM ExchangeRateHistory h
            WHERE h.source = :source
              AND (:currency IS NULL OR h.currencyCode = :currency)
              AND h.fetchedAt >= :since
            ORDER BY h.fetchedAt DESC
            """)
    List<ExchangeRateHistory> findHistory(
            @Param("source") String source,
            @Param("currency") String currency,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM ExchangeRateHistory h WHERE h.fetchedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
