package com.tappy.pos.model.entity.pawn;

import com.tappy.pos.model.enums.PawnStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pawn_audit")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PawnAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_time")
    private LocalDateTime actionTime;

    @Column(name = "pawn_id")
    private Long pawnId;

    @Column(name = "customer_id")
    private Long customerId;

    //Name or description of the pawned item.
    @Column(name = "item_name")
    private String itemName;

    //Additional details about the item.
    @Column(name = "item_description")
    private String itemDescription;

    //Weight of the item(For Gold item)
    @Column(name = "item_weight")
    private BigDecimal itemWeight;

    @Column(name = "gem_weight")
    private BigDecimal gemWeight;

    //Estimated value of the item
    @Column(name = "item_value")
    private BigDecimal itemValue;

    //Type of the item
    @Column(name = "item_type")
    private String itemType;

    //Brand of the item
    @Column(name = "item_brand")
    private String itemBrand;

    //Date when the item was pawned.
    @Column(name = "pawn_date")
    private LocalDateTime pawnDate;

    //Date by which the customer must redeem the item.
    @Column(name = "pawn_due_date")
    private LocalDateTime pawnDueDate;

    //Amount loaned to the customer for the pawned item.
    @Column(name = "pawn_amount")
    private BigDecimal pawnAmount;

    //Interest rate applied to the pawn.
    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    //Current status of the pawned item.
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PawnStatus pawnStatus;

    @Column(name = "canceled_reason")
    private String canceledReason;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "redeem_date")
    private LocalDateTime redeemDate;

    @Column(name = "interest_amount")
    private BigDecimal interestAmount;

    @Column(name = "forfeited_reason")
    private String forfeitedReason;

    @Column(name = "forfeited_amount")
    private BigDecimal forfeitedAmount;

    @Column(name = "forfeited_date")
    private LocalDateTime forfeitedDate;

    @Column(name = "original_id")
    private Long originalId;

    @Column(name = "interest_calc_mode")
    private String interestCalcMode;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
