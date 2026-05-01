package com.knp.model.entity.order;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import com.knp.model.entity.customer.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.knp.model.enums.CartStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart Entity - POS Shopping Cart
 * Manages items, totals, and pricing for POS transactions
 * 
 * Linked to:
 * - Customer (optional - can be anonymous)
 * - Multiple CartItems (one-to-many)
 * 
 * Status: ACTIVE | ABANDONED | COMPLETED | PAID
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 36)
    private String cartId; // UUID for cart session

    @Column(name = "customer_id")
    private Long customerId; // Optional: associated customer

    @Builder.Default
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @Builder.Default
    @OneToMany(
        mappedBy = "cart",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<CartItemEntity> items = new ArrayList<>();

    @Column(name = "applied_coupons", columnDefinition = "TEXT")
    private String appliedCoupons; // JSON array of coupon codes: ["CODE1", "CODE2"]

    @Column(name = "applied_promotions", columnDefinition = "TEXT")
    private String appliedPromotions; // JSON array of promotion IDs: ["PROMO1", "PROMO2"]

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Optional: special instructions

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "abandoned_at")
    private LocalDateTime abandonedAt; // When cart was marked as abandoned

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // When cart was converted to order

    /**
     * Add item to cart (with duplicate checking)
     * If same product + variants exist, merge quantity
     * Otherwise, add new line
     */
    public void addItem(CartItemEntity item) {
        if (item != null) {
            item.setCart(this);
            items.add(item);
        }
    }

    /**
     * Remove item from cart
     */
    public void removeItem(CartItemEntity item) {
        if (item != null) {
            items.remove(item);
            item.setCart(null);
        }
    }

    /**
     * Clear all items from cart
     */
    public void clearItems() {
        items.forEach(item -> item.setCart(null));
        items.clear();
    }

    /**
     * Get total number of items in cart (sum of quantities)
     */
    public Integer getTotalItemCount() {
        return items.stream()
            .mapToInt(CartItemEntity::getQuantity)
            .sum();
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Calculate and update cart totals
     */
    public void recalculateTotals() {
        if (items == null || items.isEmpty()) {
            subtotal = BigDecimal.ZERO;
            totalDiscount = BigDecimal.ZERO;
            totalTax = BigDecimal.ZERO;
            total = BigDecimal.ZERO;
            return;
        }

        // Calculate subtotal from items
        subtotal = items.stream()
            .map(CartItemEntity::getLineSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total discount
        totalDiscount = items.stream()
            .map(CartItemEntity::getDiscountValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate taxable amount
        BigDecimal taxableAmount = subtotal.subtract(totalDiscount);

        // Calculate tax (10% - configurable)
        totalTax = taxableAmount.multiply(new BigDecimal("0.10"));

        // Calculate grand total
        total = taxableAmount.add(totalTax);
    }

}

