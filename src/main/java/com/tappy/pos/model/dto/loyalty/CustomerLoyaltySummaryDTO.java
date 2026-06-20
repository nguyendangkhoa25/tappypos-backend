package com.tappy.pos.model.dto.loyalty;

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
    // Stamp card ("mua N ly tặng 1")
    private Boolean stampCardEnabled;
    private Integer stampCount;       // stamps on the current card
    private Integer stampCardSize;    // stamps needed to fill a card
    private Integer stampRewards;     // filled cards available to redeem
    private String stampCardReward;   // reward description
}
