package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank(message = "Service/Product name is required")
    @Column(nullable = false)
    private String productName;

    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    @Positive(message = "Unit price must be positive")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @PrePersist
    @PreUpdate
    private void calculateTotalPrice() {
        if (this.unitPrice != null && this.quantity != null) {
            this.totalPrice = this.unitPrice.multiply(new BigDecimal(this.quantity));
        }
    }
}

