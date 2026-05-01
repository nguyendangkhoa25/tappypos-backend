package com.knp.model.entity.customer;

import com.knp.model.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;

@Entity
@Table(name = "loyalty_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction extends TenantAwareEntity {

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

    @Builder.Default
    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore = 0;

    @Builder.Default
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter = 0;

    @Column(length = 500)
    private String description;
}
