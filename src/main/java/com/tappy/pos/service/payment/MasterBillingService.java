package com.tappy.pos.service.payment;

import com.tappy.pos.model.dto.dashboard.MasterBillingStatsDTO;
import com.tappy.pos.model.dto.dashboard.PaymentLedgerItemDTO;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Read-side of the master Billing &amp; Revenue cockpit: platform-wide revenue stats + a cross-tenant
 * payment ledger. Mutations (refund, record-offline) live in {@link SubscriptionPaymentService}.
 */
public interface MasterBillingService {

    /** Platform-wide billing stats: revenue windows, MRR, by-plan/provider, pending/failed counts. */
    MasterBillingStatsDTO getStats();

    /** Paged cross-tenant payment ledger; every filter is optional (null = no constraint). */
    Page<PaymentLedgerItemDTO> getPayments(PaymentStatus status, PaymentProvider provider, String planCode,
                                           String tenantId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
