package com.knp.model.dto.loyalty;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransactionDTO {
    private Long id;
    private Long customerId;
    private Long orderId;
    private String type;
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}
