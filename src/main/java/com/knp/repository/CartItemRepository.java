package com.knp.repository;

import com.knp.model.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Cart Item Repository
 * Data access layer for CartItem operations
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    
    /**
     * Find all items in a cart
     */
    List<CartItemEntity> findByCartId(Long cartId);
    
    /**
     * Find item by product in a specific cart
     */
    List<CartItemEntity> findByCartIdAndProductId(Long cartId, Long productId);
}

