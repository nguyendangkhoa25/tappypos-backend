package com.tappy.pos.model.dto.loyalty;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLoyaltyTierRequest {
    private String name;
    private BigDecimal minSpend;
    private BigDecimal pointsMultiplier;
    private String color;
    private String description;
    private Integer sortOrder;
}
