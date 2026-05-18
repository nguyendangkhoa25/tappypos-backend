package com.tappy.pos.model.dto.pawn;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReqMoneyResponse {
    private Long pawnId;
    private Long requestId;
    private LocalDateTime requestDate;
    private BigDecimal requestAmount;
    private BigDecimal interestAmount;
    private Long heldDays;
}
