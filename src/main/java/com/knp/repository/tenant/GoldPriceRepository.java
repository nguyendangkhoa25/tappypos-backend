package com.knp.repository.tenant;

import com.knp.model.entity.tenant.GoldPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {

    @Query("SELECT g FROM GoldPrice g WHERE g.deleted = false ORDER BY g.displayOrder ASC")
    List<GoldPrice> findAllActive();

    @Query("SELECT g FROM GoldPrice g WHERE g.deleted = false AND g.showInBoard = true ORDER BY g.displayOrder ASC")
    List<GoldPrice> findAllVisibleInBoard();
}
