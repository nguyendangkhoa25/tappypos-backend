package com.tappy.pos.model.dto.pawn;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReqMoneyRequest {
    private LocalDate requestDate;
    private BigDecimal requestAmount;
}
