package com.knp.model.dto.pawn;

import com.knp.model.dto.customer.CustomerDTO;
import com.knp.model.enums.PawnStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class PawnResponse {
    private Long pawnId;
    private Long customerId;
    private String itemName;
    private String itemBrand;
    private String itemType;
    private String itemDescription;
    private BigDecimal itemValue;
    private BigDecimal itemWeight;
    private BigDecimal gemWeight;
    private LocalDateTime pawnDate;
    private LocalDateTime pawnDueDate;
    private BigDecimal pawnAmount;
    private BigDecimal interestRate;
    private String customerName;
    private String phone;
    private LocalDateTime redeemDate;
    private BigDecimal interestAmount;
    private BigDecimal mainInterestAmount;
    private BigDecimal totalAmount;
    private long heldDays;
    private PawnStatus pawnStatus;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String canceledReason;
    private String forfeitedReason;
    private LocalDateTime forfeitedDate;
    private BigDecimal forfeitedAmount;
    private CustomerDTO customer;
    private Set<ReqMoneyResponse> reqMoneys;
    private Integer interestDaysPerMonth;
    private Boolean visible;
    private List<PawnAudit> audits;
}
