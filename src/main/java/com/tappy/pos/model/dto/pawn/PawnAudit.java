package com.tappy.pos.model.dto.pawn;

import com.tappy.pos.model.enums.PawnStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PawnAudit {
    private Long actionId;
    private String actionType;
    private LocalDateTime actionTime;
    private Long customerId;
    private String itemName;
    private String itemDescription;
    private BigDecimal itemWeight;
    private BigDecimal gemWeight;
    private BigDecimal itemValue;
    private String itemType;
    private String itemBrand;
    private LocalDateTime pawnDate;
    private LocalDateTime pawnDueDate;
    private BigDecimal pawnAmount;
    private BigDecimal interestRate;
    private PawnStatus pawnStatus;
    private String canceledReason;
    private BigDecimal totalAmount;
    private LocalDateTime redeemDate;
    private BigDecimal interestAmount;
    private String forfeitedReason;
    private BigDecimal forfeitedAmount;
    private LocalDateTime forfeitedDate;
    private String interestCalcMode;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
