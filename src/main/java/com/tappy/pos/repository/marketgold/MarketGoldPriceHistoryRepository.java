package com.tappy.pos.repository.marketgold;

import com.tappy.pos.model.entity.marketgold.MarketGoldPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketGoldPriceHistoryRepository extends JpaRepository<MarketGoldPriceHistory, Long> {

    @Query("""
            SELECT h FROM MarketGoldPriceHistory h
            WHERE (:source IS NULL OR h.source = :source)
              AND (:ktype IS NULL OR h.ktype = :ktype)
              AND h.fetchedAt >= :since
            ORDER BY h.fetchedAt DESC
            """)
    List<MarketGoldPriceHistory> findHistory(
            @Param("source") String source,
            @Param("ktype") String ktype,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM MarketGoldPriceHistory h WHERE h.fetchedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
