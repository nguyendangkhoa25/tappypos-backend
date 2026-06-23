package com.tappy.pos.model.entity.finance;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A repayment (thu nợ) applied to one customer debt. The service allocates a customer-level
 * repayment to the oldest open debts first, creating one DebtPayment per debt it touches.
 */
@Entity
@Table(name = "debt_payment", indexes = {
        @Index(name = "idx_debt_payment_customer_jpa", columnList = "tenant_id, customer_id"),
        @Index(name = "idx_debt_payment_debt_jpa",     columnList = "tenant_id, debt_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DebtPayment extends TenantAwareEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "debt_id")
    private Long debtId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "method", nullable = false, length = 30)
    private String method = "CASH";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 255, nullable = false)
    private String createdBy;
}
