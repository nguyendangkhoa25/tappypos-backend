package com.knp.model.entity.customer;

import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTier extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** Minimum lifetime spend (VND) required to reach this tier */
    @Builder.Default
    @Column(name = "min_spend", nullable = false)
    private BigDecimal minSpend = BigDecimal.ZERO;

    /** Point multiplier applied when earning points at this tier (1.0 = normal, 1.5 = 50% bonus) */
    @Builder.Default
    @Column(name = "points_multiplier", nullable = false)
    private BigDecimal pointsMultiplier = BigDecimal.ONE;

    /** Display color hex code */
    @Builder.Default
    @Column(length = 20)
    private String color = "#9E9E9E";

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
