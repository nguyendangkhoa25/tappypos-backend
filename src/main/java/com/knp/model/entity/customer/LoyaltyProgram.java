package com.knp.model.entity.customer;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProgram extends TenantAwareEntity {

    /** Points earned per amountPerPoints VND spent (default: 1 point per 10,000 VND) */
    @Builder.Default
    @Column(name = "points_per_amount", nullable = false)
    private Integer pointsPerAmount = 1;

    /** Amount of VND required to earn pointsPerAmount points */
    @Builder.Default
    @Column(name = "amount_per_points", nullable = false)
    private Long amountPerPoints = 10000L;

    /** Points required to redeem for redemptionDiscountAmount */
    @Builder.Default
    @Column(name = "redemption_points_per_discount", nullable = false)
    private Integer redemptionPointsPerDiscount = 100;

    /** Discount amount (VND) received when redeeming redemptionPointsPerDiscount points */
    @Builder.Default
    @Column(name = "redemption_discount_amount", nullable = false)
    private BigDecimal redemptionDiscountAmount = new BigDecimal("10000");

    /** Minimum points a customer must have before redeeming */
    @Builder.Default
    @Column(name = "min_redemption_points", nullable = false)
    private Integer minRedemptionPoints = 100;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
