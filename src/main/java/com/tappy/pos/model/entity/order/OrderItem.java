package com.tappy.pos.model.entity.order;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "variant_id")
    private Long variantId;

    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    /** Chosen sell unit (e.g. "bao"); null = product base unit. */
    @Column(name = "sell_unit", length = 20)
    private String sellUnit;

    /** Base units per sell unit (e.g. 50 → 1 bao = 50 kg); null/1 = normal single-unit line. */
    @Column(name = "unit_factor", precision = 15, scale = 3)
    private BigDecimal unitFactor;

    @Positive(message = "Unit price must be positive")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cost_amount", nullable = false, precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_before_tax", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal amountBeforeTax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    @Column(name = "assigned_employee_name")
    private String assignedEmployeeName;

    @Builder.Default
    @Column(name = "commission_rate", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "included_in_salary_id")
    private Long includedInSalaryId;

    @Builder.Default
    @Column(name = "is_salary_calculated", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean salaryCalculated = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'STANDARD'")
    private ItemType itemType = ItemType.STANDARD;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /** Chosen modifier options as JSON: [{groupName, optionName, priceDelta}] (FnB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifiers", columnDefinition = "jsonb")
    private String modifiers;

    /** Per-item customer note (e.g. "ít đường", "không hành", "thêm đá"). */
    @Column(name = "note", length = 500)
    private String note;

    /** True when this line was a prescription-required drug (pharmacy dispensing paper trail). */
    @Builder.Default
    @Column(name = "prescription_required", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean prescriptionRequired = false;

    /** Combo this item was sold as part of; null for standalone items. */
    @Column(name = "combo_id")
    private Long comboId;

    /** Service duration in minutes (snapshot from product at order time). 0 = no timer. */
    @Builder.Default
    @Column(name = "duration_minutes", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer durationMinutes = 0;

    public enum ItemType {
        STANDARD,
        GOLD_IN,
        GOLD_OUT
    }

    public enum ItemStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED
    }

    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        if (this.unitPrice != null && this.quantity != null) {
            this.amount = this.unitPrice.multiply(new BigDecimal(this.quantity));
        }
        if (this.unitCost != null && this.quantity != null) {
            this.costAmount = this.unitCost.multiply(new BigDecimal(this.quantity));
        }
    }

    /** Quantity expressed in the product's BASE unit for stock deduction: round(quantity × unitFactor). */
    public long baseQuantity() {
        int qty = quantity != null ? quantity : 0;
        if (unitFactor == null || unitFactor.signum() <= 0) return qty;
        return unitFactor.multiply(BigDecimal.valueOf(qty)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}

