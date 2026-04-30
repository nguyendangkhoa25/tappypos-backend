package com.knp.model.entity.pawn;

import com.knp.model.enums.PawnStatus;
import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import com.knp.model.entity.customer.Customer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pawn")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PawnQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pawn_id")
    private Long pawnId;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

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

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "canceled_reason")
    private String canceledReason;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "redeem_date")
    private LocalDateTime redeemDate;

    @Column(name = "interest_amount")
    private BigDecimal interestAmount;

    @Column(name = "interest_days_per_month")
    private Integer interestDaysPerMonth;

    @Column(name = "forfeited_reason")
    private String forfeitedReason;

    @Column(name = "forfeited_amount")
    private BigDecimal forfeitedAmount;

    @Column(name = "forfeited_date")
    private LocalDateTime forfeitedDate;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "pawn_id")
    @OrderBy("requestDate")
    private Set<ReqMoneyEntity> reqMoneys = new HashSet<>();

    @Column(name = "visible")
    private Boolean visible;
}
