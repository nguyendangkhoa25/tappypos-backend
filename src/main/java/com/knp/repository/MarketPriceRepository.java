package com.knp.repository;

import com.knp.model.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    @Query("SELECT m FROM MarketPrice m WHERE m.deleted = false ORDER BY m.sortOrder ASC, m.name ASC")
    List<MarketPrice> findAllActive();

    @Query("SELECT m FROM MarketPrice m WHERE m.deleted = false AND m.isActive = true ORDER BY m.sortOrder ASC, m.name ASC")
    List<MarketPrice> findAllActiveEnabled();
}
