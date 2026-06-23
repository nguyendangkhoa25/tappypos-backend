package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    /** A customer's repayment history, most recent first. */
    List<DebtPayment> findByCustomerIdAndTenantIdOrderByPaidAtDesc(Long customerId, String tenantId);
}
