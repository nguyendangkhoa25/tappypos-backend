package com.tappy.pos.model.entity.payment;

import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A subscription renewal payment. This is a PLATFORM-level (master) table — billing is not
 * tenant-isolated data, so it carries no RLS policy; queries filter by {@code tenantId} explicitly.
 * The amount is always derived server-side from {@link com.tappy.pos.model.enums.SubscriptionPlan}.
 */
@Entity
@Table(name = "subscription_payment",
       uniqueConstraints = @UniqueConstraint(name = "uq_subscription_payment_txn_ref", columnNames = "provider_txn_ref"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "plan_code", nullable = false, length = 20)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 10)
    private BillingCycle billingCycle;

    /** Amount in VND (đồng), no decimals. */
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Our unique reference passed to the provider and echoed back on the callback (idempotency key). */
    @Column(name = "provider_txn_ref", nullable = false, length = 100)
    private String providerTxnRef;

    @Column(length = 255)
    private String description;

    /** Raw provider callback payload, for audit/debugging. */
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
