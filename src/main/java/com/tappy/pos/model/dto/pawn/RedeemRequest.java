package com.tappy.pos.model.dto.pawn;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RedeemRequest {
    private LocalDateTime redeemDate;
    private BigDecimal additionalAmount;
    private boolean extendingRequest = false;
    private String interestCalcMode;
}
