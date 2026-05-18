package com.tappy.pos.repository.order;

import com.tappy.pos.model.entity.order.CartEntity;
import com.tappy.pos.model.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cart Repository
 * Data access layer for Cart operations
 */
@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {
    
    /**
     * Find cart by unique cartId (UUID)
     */
    Optional<CartEntity> findByCartId(String cartId);
    
    /**
     * Find active cart for customer
     */
    Optional<CartEntity> findByCustomerIdAndStatus(Long customerId, CartStatus status);
    
    /**
     * Find all active carts
     */
    List<CartEntity> findByStatus(CartStatus status);
    
    /**
     * Find abandoned carts (older than specified date)
     * Used for cleanup/analytics
     */
    @Query(value = "SELECT c FROM CartEntity c WHERE c.status = 'ABANDONED' AND c.updatedAt < ?1")
    List<CartEntity> findAbandonedCarts(LocalDateTime beforeDate);
    
    /**
     * Find all carts for customer
     */
    List<CartEntity> findByCustomerId(Long customerId);
    
    /**
     * Count active carts
     */
    Long countByStatus(CartStatus status);
}

