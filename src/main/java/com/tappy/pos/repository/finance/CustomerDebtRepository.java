package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.CustomerDebt;
import com.tappy.pos.model.enums.DebtStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Tenant isolation is enforced explicitly: derived finders carry tenantId in the WHERE,
 * native aggregates use AND tenant_id = :tenantId. Never rely on RLS alone.
 */
@Repository
public interface CustomerDebtRepository extends JpaRepository<CustomerDebt, Long> {

    Optional<CustomerDebt> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    /** A customer's full debt history (statement), oldest first. */
    List<CustomerDebt> findByCustomerIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(Long customerId, String tenantId);

    /** A customer's still-owing debts (not PAID), oldest first — used for repayment allocation. */
    List<CustomerDebt> findByCustomerIdAndTenantIdAndStatusNotAndDeletedFalseOrderByCreatedAtAsc(
            Long customerId, String tenantId, DebtStatus status);

    /** Per-customer outstanding balances: [customerId, customerName, totalOutstanding, debtCount, earliestDueDate]. */
    @Query(value = "SELECT customer_id, MAX(customer_name), COALESCE(SUM(outstanding_amount), 0), " +
           "COUNT(*), MIN(due_date) " +
           "FROM customer_debt WHERE deleted = FALSE AND tenant_id = :tenantId AND status <> 'PAID' " +
           "GROUP BY customer_id ORDER BY COALESCE(SUM(outstanding_amount), 0) DESC",
           nativeQuery = true)
    List<Object[]> findOutstandingBalancesByCustomer(@Param("tenantId") String tenantId);

    /** Total outstanding debt across all customers (dashboard / shop-wide). */
    @Query(value = "SELECT COALESCE(SUM(outstanding_amount), 0) FROM customer_debt " +
           "WHERE deleted = FALSE AND tenant_id = :tenantId AND status <> 'PAID'",
           nativeQuery = true)
    BigDecimal sumTotalOutstanding(@Param("tenantId") String tenantId);

    /** A single customer's outstanding total. */
    @Query(value = "SELECT COALESCE(SUM(outstanding_amount), 0) FROM customer_debt " +
           "WHERE deleted = FALSE AND tenant_id = :tenantId AND customer_id = :customerId AND status <> 'PAID'",
           nativeQuery = true)
    BigDecimal sumOutstandingByCustomer(@Param("tenantId") String tenantId, @Param("customerId") Long customerId);

    // ── Installment (trả góp) contracts — a debt with installmentCount NOT NULL. ──
    @Query("SELECT d FROM CustomerDebt d WHERE d.deleted = false AND d.tenantId = :tenantId " +
           "AND d.installmentCount IS NOT NULL ORDER BY d.createdAt DESC")
    Page<CustomerDebt> findInstallments(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT d FROM CustomerDebt d WHERE d.deleted = false AND d.tenantId = :tenantId " +
           "AND d.createdBy = :createdBy AND d.installmentCount IS NOT NULL ORDER BY d.createdAt DESC")
    Page<CustomerDebt> findInstallmentsByCreatedBy(@Param("tenantId") String tenantId,
                                                   @Param("createdBy") String createdBy, Pageable pageable);
}
