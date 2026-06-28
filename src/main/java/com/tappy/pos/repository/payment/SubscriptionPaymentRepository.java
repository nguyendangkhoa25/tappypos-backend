package com.tappy.pos.repository.payment;

import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for subscription payments (master table — explicit tenant filtering, no RLS).
 *
 * The aggregation + paged-search queries below back the master Billing &amp; Revenue cockpit; they
 * span all tenants (safe: this table carries no RLS policy — see {@link SubscriptionPayment}).
 */
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByProviderTxnRef(String providerTxnRef);

    List<SubscriptionPayment> findTop50ByTenantIdOrderByCreatedAtDesc(String tenantId);

    // ── Master cross-tenant ledger + revenue aggregation ───────────────────────

    /** Paged ledger across all tenants; every filter is optional (null = no constraint). */
    @Query("""
            SELECT p FROM SubscriptionPayment p
            WHERE (:status   IS NULL OR p.status   = :status)
              AND (:provider IS NULL OR p.provider = :provider)
              AND (:planCode IS NULL OR p.planCode = :planCode)
              AND (:tenantId IS NULL OR p.tenantId = :tenantId)
              AND (:from     IS NULL OR p.createdAt >= :from)
              AND (:to       IS NULL OR p.createdAt <  :to)
            ORDER BY p.createdAt DESC
            """)
    Page<SubscriptionPayment> search(@Param("status") PaymentStatus status,
                                     @Param("provider") PaymentProvider provider,
                                     @Param("planCode") String planCode,
                                     @Param("tenantId") String tenantId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     Pageable pageable);

    /** Total collected (VND) from PAID payments whose paid_at falls in [from, to). */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM SubscriptionPayment p
            WHERE p.status = com.tappy.pos.model.enums.PaymentStatus.PAID
              AND p.paidAt >= :from AND p.paidAt < :to
            """)
    long sumAmountPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Count of PAID payments whose paid_at falls in [from, to). */
    @Query("""
            SELECT COUNT(p) FROM SubscriptionPayment p
            WHERE p.status = com.tappy.pos.model.enums.PaymentStatus.PAID
              AND p.paidAt >= :from AND p.paidAt < :to
            """)
    long countPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatus(PaymentStatus status);

    /** PAID revenue grouped by plan code → rows of [planCode, count, sumAmount]. */
    @Query("""
            SELECT p.planCode, COUNT(p), COALESCE(SUM(p.amount), 0) FROM SubscriptionPayment p
            WHERE p.status = com.tappy.pos.model.enums.PaymentStatus.PAID
            GROUP BY p.planCode ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> revenueByPlan();

    /** PAID revenue grouped by provider → rows of [provider, count, sumAmount]. */
    @Query("""
            SELECT p.provider, COUNT(p), COALESCE(SUM(p.amount), 0) FROM SubscriptionPayment p
            WHERE p.status = com.tappy.pos.model.enums.PaymentStatus.PAID
            GROUP BY p.provider ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> revenueByProvider();
}
