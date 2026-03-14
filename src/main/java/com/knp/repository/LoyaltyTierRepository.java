package com.knp.repository;

import com.knp.model.entity.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, Long> {

    @Query("SELECT t FROM LoyaltyTier t WHERE t.deleted = false ORDER BY t.minSpend ASC")
    List<LoyaltyTier> findAllActive();

    @Query("SELECT t FROM LoyaltyTier t WHERE t.deleted = false AND t.minSpend <= :spend ORDER BY t.minSpend DESC LIMIT 1")
    Optional<LoyaltyTier> findTierForSpend(@org.springframework.data.repository.query.Param("spend") BigDecimal spend);

    @Query("SELECT t FROM LoyaltyTier t WHERE t.deleted = false AND t.minSpend > :spend ORDER BY t.minSpend ASC LIMIT 1")
    Optional<LoyaltyTier> findNextTierForSpend(@org.springframework.data.repository.query.Param("spend") BigDecimal spend);
}
