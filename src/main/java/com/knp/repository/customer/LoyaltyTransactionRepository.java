package com.knp.repository.customer;

import com.knp.model.entity.customer.LoyaltyTransaction;
import com.knp.model.enums.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    @Query("SELECT t FROM LoyaltyTransaction t WHERE t.customerId = :customerId AND t.deleted = false ORDER BY t.createdAt DESC")
    Page<LoyaltyTransaction> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT t FROM LoyaltyTransaction t WHERE t.orderId = :orderId AND t.deleted = false ORDER BY t.createdAt DESC LIMIT 1")
    Optional<LoyaltyTransaction> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT t FROM LoyaltyTransaction t WHERE t.customerId = :customerId AND t.type = com.knp.model.enums.LoyaltyTransactionType.REDEEMED AND t.orderId IS NULL AND t.deleted = false ORDER BY t.createdAt DESC LIMIT 1")
    Optional<LoyaltyTransaction> findTopRedemptionWithoutOrder(@Param("customerId") Long customerId);
}
