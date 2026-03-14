package com.knp.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.knp.model.enums.DiscountType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cart Item Entity - Individual line item in shopping cart
 * Represents one product (with variants) in the cart
 * 
 * Linked to:
 * - CartEntity (many-to-one)
 * - ProductEntity (via product_id)
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType = DiscountType.NONE;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_reason")
    private String discountReason;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "line_grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineGrandTotal = BigDecimal.ZERO;

    @Column(name = "variants", columnDefinition = "JSON")
    private String variants; // JSON object: {"size": "M", "color": "Red"}

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        recalculateLineTotal();
    }

    @PostLoad
    protected void onLoad() {
        recalculateLineTotal();
    }

    /**
     * Recalculate line totals based on quantity and pricing
     */
    public void recalculateLineTotal() {
        // Line subtotal = quantity * base price
        lineSubtotal = basePrice.multiply(new BigDecimal(quantity));

        // Line total = line subtotal - discount
        lineTotal = lineSubtotal.subtract(discountValue);

        // Tax (10% of line total)
        tax = lineTotal.multiply(new BigDecimal("0.10"));

        // Line grand total = line total + tax
        lineGrandTotal = lineTotal.add(tax);
    }

    /**
     * Apply discount to this line item
     * @param type AMOUNT or PERCENTAGE
     * @param value Discount amount or percentage value
     */
    public void applyDiscount(DiscountType type, BigDecimal value, String reason) {
        this.discountType = type;
        this.discountReason = reason;

        if (type == DiscountType.AMOUNT) {
            this.discountValue = value;
        } else if (type == DiscountType.PERCENTAGE) {
            // Calculate discount amount from percentage
            this.discountValue = lineSubtotal
                .multiply(value)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }

        recalculateLineTotal();
    }

    /**
     * Remove discount from this line item
     */
    public void removeDiscount() {
        this.discountType = DiscountType.NONE;
        this.discountValue = BigDecimal.ZERO;
        this.discountReason = null;
        recalculateLineTotal();
    }

}

