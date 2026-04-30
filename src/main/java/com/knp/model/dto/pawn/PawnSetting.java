package com.knp.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PawnSetting {
    private BigDecimal interestRate;
    private int interestType;
    private int dueDate;
}
