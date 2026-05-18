package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerPawnKPI {
    private Long customerId;
    private String lastName;
    private String phone;
    private int pawnCount;
    private long pawnAmount;
    private long interestAmount;
}
