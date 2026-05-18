package com.tappy.pos.model.entity.order;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.tappy.pos.model.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tappy.pos.model.enums.DiscountType;
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
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_type_code", length = 50)
    private String productTypeCode;

    @Column(name = "sku")
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType = DiscountType.NONE;

    @Builder.Default
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_reason")
    private String discountReason;

    @Builder.Default
    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "line_grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineGrandTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = new BigDecimal("0.10");

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variants", columnDefinition = "jsonb")
    private String variants;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    @Column(name = "assigned_employee_name")
    private String assignedEmployeeName;

    @Builder.Default
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 10, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'STANDARD'")
    private ItemType itemType = ItemType.STANDARD;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ItemType {
        STANDARD,
        GOLD_IN,
        GOLD_OUT
    }

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

        // Tax — rate comes from the cart's configured tax_rate (0.0 for jewellery shops, 0.10 for most others)
        tax = lineTotal.multiply(taxRate != null ? taxRate : BigDecimal.ZERO);

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

