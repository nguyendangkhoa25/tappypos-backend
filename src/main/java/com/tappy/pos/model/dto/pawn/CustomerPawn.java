package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerPawn {
    private int pawnedCount;
    private long pawnedAmount;
    private int totalCount;
    private long totalAmount;
    private long interestAmount;
}
