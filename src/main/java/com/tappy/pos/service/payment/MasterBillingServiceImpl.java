package com.tappy.pos.service.payment;

import com.tappy.pos.model.dto.dashboard.MasterBillingStatsDTO;
import com.tappy.pos.model.dto.dashboard.PaymentLedgerItemDTO;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.model.enums.SubscriptionPlan;
import com.tappy.pos.repository.payment.SubscriptionPaymentRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasterBillingServiceImpl implements MasterBillingService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public MasterBillingStatsDTO getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();

        return MasterBillingStatsDTO.builder()
                .revenueToday(paymentRepository.sumAmountPaidBetween(dayStart, dayStart.plusDays(1)))
                .revenueThisMonth(paymentRepository.sumAmountPaidBetween(monthStart, monthStart.plusMonths(1)))
                .revenueThisYear(paymentRepository.sumAmountPaidBetween(yearStart, yearStart.plusYears(1)))
                .paidCountThisMonth(paymentRepository.countPaidBetween(monthStart, monthStart.plusMonths(1)))
                .pendingCount(paymentRepository.countByStatus(PaymentStatus.PENDING))
                .failedCount(paymentRepository.countByStatus(PaymentStatus.FAILED))
                .mrr(computeMrr())
                .byPlan(paymentRepository.revenueByPlan().stream()
                        .map(r -> MasterBillingStatsDTO.PlanRevenue.builder()
                                .planCode((String) r[0])
                                .count(((Number) r[1]).longValue())
                                .amount(((Number) r[2]).longValue())
                                .build())
                        .toList())
                .byProvider(paymentRepository.revenueByProvider().stream()
                        .map(r -> MasterBillingStatsDTO.ProviderRevenue.builder()
                                .provider(String.valueOf(r[0]))
                                .count(((Number) r[1]).longValue())
                                .amount(((Number) r[2]).longValue())
                                .build())
                        .toList())
                .build();
    }

    /**
     * MRR ≈ sum over active, non-expired tenants of their plan's monthly price (a yearly subscriber
     * still represents one month's worth of recurring revenue). A null/blank plan resolves to the
     * default (BASIC); an unknown code is skipped so one bad row can't break the figure.
     */
    private long computeMrr() {
        long mrr = 0;
        for (Tenant t : tenantRepository.findAllByActiveTrue()) {
            if (t.isExpired()) continue;
            try {
                mrr += SubscriptionPlan.of(t.getSubscriptionType()).pricePerMonth();
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping tenant {} in MRR — unknown plan '{}'", t.getTenantId(), t.getSubscriptionType());
            }
        }
        return mrr;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentLedgerItemDTO> getPayments(PaymentStatus status, PaymentProvider provider, String planCode,
                                                  String tenantId, LocalDateTime from, LocalDateTime to,
                                                  Pageable pageable) {
        Page<com.tappy.pos.model.entity.payment.SubscriptionPayment> page =
                paymentRepository.search(status, provider, planCode, tenantId, from, to, pageable);

        // Resolve tenant names once per distinct tenant on this page (ledger pages are small).
        Map<String, String> names = new HashMap<>();
        page.getContent().forEach(p -> names.computeIfAbsent(p.getTenantId(),
                id -> tenantRepository.findByTenantId(id).map(Tenant::getName).orElse(id)));

        return page.map(p -> PaymentLedgerItemDTO.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .tenantName(names.get(p.getTenantId()))
                .provider(p.getProvider().name())
                .planCode(p.getPlanCode())
                .billingCycle(p.getBillingCycle().name())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .txnRef(p.getProviderTxnRef())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build());
    }
}
