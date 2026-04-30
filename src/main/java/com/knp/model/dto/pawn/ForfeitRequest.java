package com.knp.model.dto.pawn;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ForfeitRequest {
    private LocalDateTime forfeitedDate;
    private BigDecimal totalAmount;
    private BigDecimal interestAmount;
    private String forfeitedReason;
    private BigDecimal forfeitedAmount;
}
