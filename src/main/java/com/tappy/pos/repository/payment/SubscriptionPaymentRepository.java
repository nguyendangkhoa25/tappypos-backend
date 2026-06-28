package com.tappy.pos.repository.payment;

import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for subscription payments (master table — explicit tenant filtering, no RLS).
 */
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByProviderTxnRef(String providerTxnRef);

    List<SubscriptionPayment> findTop50ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
