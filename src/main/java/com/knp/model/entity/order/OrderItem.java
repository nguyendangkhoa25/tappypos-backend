package com.knp.model.entity.order;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import com.knp.model.entity.product.Product;
import jakarta.validation.constraints.Positive;
import lombok.*;

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

    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    @Positive(message = "Unit price must be positive")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cost_amount", nullable = false, precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_before_tax", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal amountBeforeTax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_rate", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @Column(name = "is_salary_calculated", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean salaryCalculated = false;

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
}

