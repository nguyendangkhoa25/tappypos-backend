package com.knp.model.dto.loyalty;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveLoyaltyProgramRequest {
    private Integer pointsPerAmount;
    private Long amountPerPoints;
    private Integer redemptionPointsPerDiscount;
    private BigDecimal redemptionDiscountAmount;
    private Integer minRedemptionPoints;
    private Boolean isActive;
}
