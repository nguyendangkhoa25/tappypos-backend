package com.tappy.pos.repository.order;

import com.tappy.pos.model.entity.order.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.deleted = false ORDER BY p.createdAt DESC")
    Page<Promotion> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.deleted = false AND p.code = :code")
    Optional<Promotion> findByCode(@Param("code") String code);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.deleted = false
          AND p.code = :code
          AND p.isActive = true
          AND (p.startDate IS NULL OR p.startDate <= :now)
          AND (p.endDate IS NULL OR p.endDate >= :now)
          AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
        """)
    Optional<Promotion> findValidPromotion(@Param("code") String code, @Param("now") LocalDateTime now);
}
