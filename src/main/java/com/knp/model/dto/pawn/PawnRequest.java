package com.knp.model.dto.pawn;

import com.knp.model.enums.PawnStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PawnRequest {
    private Long customerId;

    @NotBlank
    private String itemName;

    private String itemDescription;
    private BigDecimal itemWeight;
    private BigDecimal gemWeight;
    private BigDecimal itemValue;
    private String itemType;
    private String itemBrand;

    @NotNull
    private LocalDateTime pawnDate;

    @NotNull
    private LocalDateTime pawnDueDate;

    @NotNull
    private BigDecimal pawnAmount;

    @NotNull
    private BigDecimal interestRate;
    private BigDecimal totalAmount;
    private PawnStatus pawnStatus;
    private LocalDateTime redeemDate;
    private BigDecimal interestAmount;
    private LocalDateTime forfeitedDate;
    private String forfeitedReason;
    private BigDecimal forfeitedAmount;
    private LocalDateTime extendDate;
    private LocalDateTime extendDueDate;
    private boolean visitingGuest;
    private String customerName;
    private List<Long> deletedRequestIds;
    private Integer interestDaysPerMonth;
    private String requestType = "";
    private long heldDays;
    private boolean visible = true;
}
