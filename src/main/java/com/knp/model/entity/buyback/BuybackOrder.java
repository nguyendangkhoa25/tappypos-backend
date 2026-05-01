package com.knp.model.entity.buyback;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import com.knp.model.entity.customer.Customer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buyback_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuybackOrder extends TenantAwareEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Builder.Default
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = "CASH";

    @Builder.Default
    @Column(name = "buy_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal buyTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sale_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal saleTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @OneToMany(mappedBy = "buybackOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BuybackOrderItem> items = new ArrayList<>();

    public enum OrderType  { BUY, EXCHANGE }
    public enum OrderStatus { PENDING, COMPLETED, CANCELLED }

    public void complete(String username) {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedBy = username;
    }

    public void cancel(String username) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = username;
    }
}
