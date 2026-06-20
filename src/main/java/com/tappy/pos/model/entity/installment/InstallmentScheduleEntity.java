package com.tappy.pos.model.entity.installment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One kỳ (period) of a trả-góp contract. The contract itself is a {@code CustomerDebt} with
 * installmentCount NOT NULL; these rows break the financed amount into per-kỳ due dates/amounts.
 * Interest-free (interestPct reserved, unused). Tenant-scoped via RLS. VEHICLE_SHOP_SHOP_TYPE_PLAN §4e.
 */
@Entity
@Table(name = "installment_schedule")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantFilterId")
public class InstallmentScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "debt_id", nullable = false)
    private Long debtId;            // FK customer_debt.id

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;  // kỳ thứ (1..N)

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "interest_pct")
    private BigDecimal interestPct; // reserved (interest-free now)

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_by")
    private String paidBy;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
