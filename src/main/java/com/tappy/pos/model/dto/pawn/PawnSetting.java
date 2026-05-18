package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PawnSetting {
    private BigDecimal interestRate;
    private int interestType;
    private int dueDate;
    /** Comma-separated accepted pawn item type codes, e.g. "GOLD,ELECTRONICS,WATCH" */
    private String acceptedTypes;
}
