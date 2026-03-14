package com.knp.model.entity;

import com.knp.model.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTransactionType type;

    /** Positive = earned/adjusted up; negative = redeemed/expired */
    @Column(nullable = false)
    private Integer points;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore = 0;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter = 0;

    @Column(length = 500)
    private String description;
}
