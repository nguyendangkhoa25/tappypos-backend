package com.knp.model.dto.loyalty;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLoyaltySummaryDTO {
    private Long customerId;
    private String customerName;
    private Integer loyaltyPoints;
    private BigDecimal totalSpent;
    private LoyaltyTierDTO currentTier;
    private LoyaltyTierDTO nextTier;
    private BigDecimal amountToNextTier;
}
