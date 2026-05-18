package com.tappy.pos.model.entity.pawn;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pawn_req_money_audit")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ReqMoneyAuditEntity {
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

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "pawn_id")
    private Long pawnId;

    @Column(name = "request_amount")
    private BigDecimal requestAmount;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
