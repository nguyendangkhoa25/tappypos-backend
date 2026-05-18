package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {
    @Query("SELECT m FROM MarketPrice m WHERE m.deletedAt IS NULL ORDER BY m.sortOrder ASC, m.name ASC")
    List<MarketPrice> findAllActive();
}
