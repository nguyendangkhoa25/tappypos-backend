package com.tappy.pos.service.payment;

import com.tappy.pos.model.dto.dashboard.MasterBillingStatsDTO;
import com.tappy.pos.model.dto.dashboard.PaymentLedgerItemDTO;
import com.tappy.pos.model.entity.payment.SubscriptionPayment;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.BillingCycle;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.repository.payment.SubscriptionPaymentRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MasterBillingServiceImpl Unit Tests")
class MasterBillingServiceImplTest {

    private final SubscriptionPaymentRepository paymentRepository = mock(SubscriptionPaymentRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);

    private final MasterBillingServiceImpl service =
            new MasterBillingServiceImpl(paymentRepository, tenantRepository);

    /** Real Tenant so isExpired() reflects the expiry date (avoids stubbing a concrete entity method). */
    private Tenant activeTenant(String type, boolean expired) {
        Tenant t = new Tenant();
        t.setSubscriptionType(type);
        t.setExpirationDate(expired ? LocalDate.now().minusDays(1) : LocalDate.now().plusYears(1));
        return t;
    }

    @Test
    @DisplayName("getStats aggregates revenue windows, by-plan/provider, and MRR over active non-expired tenants")
    void getStats_aggregates() {
        when(paymentRepository.sumAmountPaidBetween(any(), any())).thenReturn(500_000L);
        when(paymentRepository.countPaidBetween(any(), any())).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(2L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(1L);
        when(paymentRepository.revenueByPlan()).thenReturn(List.<Object[]>of(
                new Object[]{"PRO", 3L, 597_000L}));
        when(paymentRepository.revenueByProvider()).thenReturn(List.<Object[]>of(
                new Object[]{PaymentProvider.VIETQR, 2L, 300_000L}));
        // MRR: BASIC (99,000) + PRO (199,000) active; one expired PRO is skipped.
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(
                activeTenant("BASIC", false), activeTenant("PRO", false), activeTenant("PRO", true)));

        MasterBillingStatsDTO stats = service.getStats();

        assertThat(stats.getRevenueToday()).isEqualTo(500_000L);
        assertThat(stats.getRevenueThisMonth()).isEqualTo(500_000L);
        assertThat(stats.getRevenueThisYear()).isEqualTo(500_000L);
        assertThat(stats.getPaidCountThisMonth()).isEqualTo(5L);
        assertThat(stats.getPendingCount()).isEqualTo(2L);
        assertThat(stats.getFailedCount()).isEqualTo(1L);
        assertThat(stats.getMrr()).isEqualTo(298_000L); // 99,000 + 199,000
        assertThat(stats.getByPlan()).hasSize(1);
        assertThat(stats.getByPlan().get(0).getPlanCode()).isEqualTo("PRO");
        assertThat(stats.getByPlan().get(0).getCount()).isEqualTo(3L);
        assertThat(stats.getByPlan().get(0).getAmount()).isEqualTo(597_000L);
        assertThat(stats.getByProvider().get(0).getProvider()).isEqualTo("VIETQR");
        assertThat(stats.getByProvider().get(0).getAmount()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("getStats: a tenant on an unknown plan is skipped in MRR rather than throwing")
    void getStats_skipsUnknownPlanInMrr() {
        when(paymentRepository.sumAmountPaidBetween(any(), any())).thenReturn(0L);
        when(paymentRepository.countPaidBetween(any(), any())).thenReturn(0L);
        when(paymentRepository.revenueByPlan()).thenReturn(List.of());
        when(paymentRepository.revenueByProvider()).thenReturn(List.of());
        when(tenantRepository.findAllByActiveTrue()).thenReturn(List.of(
                activeTenant("BASIC", false), activeTenant("WEIRD_PLAN", false)));

        assertThat(service.getStats().getMrr()).isEqualTo(99_000L); // only BASIC counts
    }

    @Test
    @DisplayName("getPayments maps payment rows to ledger DTOs and resolves the tenant name")
    void getPayments_mapsAndResolvesName() {
        SubscriptionPayment p = SubscriptionPayment.builder()
                .id(7L).tenantId("shop1").provider(PaymentProvider.MOMO).planCode("PRO")
                .billingCycle(BillingCycle.YEARLY).amount(1_990_000).status(PaymentStatus.PAID)
                .providerTxnRef("TXN9").description("note").createdAt(LocalDateTime.now()).build();
        Page<SubscriptionPayment> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);
        when(paymentRepository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
        Tenant tenant = mock(Tenant.class);
        when(tenant.getName()).thenReturn("Shop One");
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));

        Page<PaymentLedgerItemDTO> result = service.getPayments(null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        PaymentLedgerItemDTO dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getTenantName()).isEqualTo("Shop One");
        assertThat(dto.getProvider()).isEqualTo("MOMO");
        assertThat(dto.getStatus()).isEqualTo("PAID");
        assertThat(dto.getAmount()).isEqualTo(1_990_000L);
        assertThat(dto.getTxnRef()).isEqualTo("TXN9");
    }

    @Test
    @DisplayName("getPayments falls back to the tenantId when no tenant row matches")
    void getPayments_unknownTenantFallsBackToId() {
        SubscriptionPayment p = SubscriptionPayment.builder()
                .id(1L).tenantId("ghost").provider(PaymentProvider.MANUAL).planCode("BASIC")
                .billingCycle(BillingCycle.MONTHLY).amount(99_000).status(PaymentStatus.PAID)
                .providerTxnRef("T1").createdAt(LocalDateTime.now()).build();
        when(paymentRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(tenantRepository.findByTenantId("ghost")).thenReturn(Optional.empty());

        Page<PaymentLedgerItemDTO> result = service.getPayments(null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent().get(0).getTenantName()).isEqualTo("ghost");
    }
}
