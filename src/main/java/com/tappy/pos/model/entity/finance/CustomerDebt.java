package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.enums.DebtStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single customer debt (công nợ) — one row per credit sale (bán chịu), linked to an
 * order when created at checkout, or stand-alone when recorded manually. A customer's
 * outstanding balance is the SUM(outstandingAmount) of their non-deleted, non-PAID debts.
 */
@Entity
@Table(name = "customer_debt", indexes = {
        @Index(name = "idx_customer_debt_customer_jpa", columnList = "tenant_id, customer_id"),
        @Index(name = "idx_customer_debt_status_jpa",   columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerDebt extends TenantAwareEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "original_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "outstanding_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DebtStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** When NOT NULL this debt is a trả-góp contract paid over this many kỳ (see installment_schedule). */
    @Column(name = "installment_count")
    private Integer installmentCount;

    /** Optional trả-trước (down payment) recorded at contract creation. */
    @Column(name = "down_payment", precision = 15, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 255, nullable = false)
    private String createdBy;
}
