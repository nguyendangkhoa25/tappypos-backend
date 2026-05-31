package com.tappy.pos.model.dto.pawn;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PawnJewelryDetail {
    private BigDecimal totalWeight;
    private BigDecimal gemWeight;
    private BigDecimal goldWeight;
    private String purity;
    private String metalType;
    private String hallmark;
}
