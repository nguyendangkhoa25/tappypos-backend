package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PawnCustomerInsights {
    private long totalCustomers;
    private long newCustomers;
    private long returningCustomers;
    private long walkInCount;
}
