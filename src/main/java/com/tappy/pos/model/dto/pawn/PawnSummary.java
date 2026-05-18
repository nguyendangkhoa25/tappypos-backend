package com.tappy.pos.model.dto.pawn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PawnSummary {
    private long totalCount;
    private BigDecimal totalWeight;
    private long totalAmount;
}
