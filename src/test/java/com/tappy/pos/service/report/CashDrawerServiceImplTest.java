package com.tappy.pos.service.report;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.dto.report.CashDrawerDTO;
import com.tappy.pos.model.dto.report.CloseDrawerRequest;
import com.tappy.pos.model.entity.finance.CashDrawerClose;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.CashDrawerCloseRepository;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashDrawerServiceImpl Unit Tests")
class CashDrawerServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PawnRepository pawnRepository;
    @Mock private ShopExpenseRepository shopExpenseRepository;
    @Mock private CashDrawerCloseRepository cashDrawerCloseRepository;
    @Mock private FeatureContext featureContext;
    @Mock private TenantContext tenantContext;

    @InjectMocks private CashDrawerServiceImpl service;

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void stubCashFlows() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        when(orderRepository.sumCashSalesByDateRange(any(), any())).thenReturn(new BigDecimal("20000000"));
        when(orderRepository.sumCashBuyByDateRange(any(), any())).thenReturn(new BigDecimal("8000000"));
        when(shopExpenseRepository.sumCashByDate(eq("shop1"), any())).thenReturn(new BigDecimal("2000000"));
    }

    private void stubPawn() {
        when(featureContext.hasFeature("PAWN")).thenReturn(true);
        when(pawnRepository.sumNewPawnAmountByDateRange(any(), any())).thenReturn(new BigDecimal("15000000"));
        when(pawnRepository.sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(List.<Object[]>of(new Object[]{ new BigDecimal("10000000"), 1L }));
        when(pawnRepository.sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(3000000L);
    }

    @Test
    @DisplayName("preview: opening carries over, expected = opening + ins − outs")
    void preview_computesExpected() {
        when(cashDrawerCloseRepository.findByBusinessDateAndDeletedFalse(any())).thenReturn(Optional.empty());
        CashDrawerClose prior = new CashDrawerClose();
        prior.setCountedAmount(new BigDecimal("5000000"));
        when(cashDrawerCloseRepository.findTopByBusinessDateLessThanAndDeletedFalseOrderByBusinessDateDesc(any()))
                .thenReturn(Optional.of(prior));
        stubCashFlows();
        stubPawn();

        CashDrawerDTO dto = service.getReconciliation(LocalDate.of(2026, 6, 13));

        assertThat(dto.getOpening()).isEqualByComparingTo("5000000");        // carried over
        assertThat(dto.getCashSales()).isEqualByComparingTo("20000000");
        assertThat(dto.getPawnRedeemed()).isEqualByComparingTo("13000000");  // 10M principal + 3M interest
        assertThat(dto.getGoldBuy()).isEqualByComparingTo("8000000");
        assertThat(dto.getPawnLoans()).isEqualByComparingTo("15000000");
        assertThat(dto.getCashExpenses()).isEqualByComparingTo("2000000");
        // 5 + 20 + 13 − 8 − 15 − 2 = 13M
        assertThat(dto.getExpected()).isEqualByComparingTo("13000000");
        assertThat(dto.isClosed()).isFalse();
    }

    @Test
    @DisplayName("close: difference = counted − expected (over), and persists the row")
    void close_persistsAndComputesDifference() {
        when(cashDrawerCloseRepository.findByBusinessDateAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(cashDrawerCloseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubCashFlows();
        stubPawn();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null));

        CloseDrawerRequest req = new CloseDrawerRequest();
        req.setDate(LocalDate.of(2026, 6, 13));
        req.setOpening(new BigDecimal("5000000"));
        req.setCounted(new BigDecimal("14000000")); // 1M over the 13M expected

        CashDrawerDTO dto = service.close(req);

        assertThat(dto.getExpected()).isEqualByComparingTo("13000000");
        assertThat(dto.getCounted()).isEqualByComparingTo("14000000");
        assertThat(dto.getDifference()).isEqualByComparingTo("1000000");
        assertThat(dto.isClosed()).isTrue();
        assertThat(dto.getClosedBy()).isEqualTo("owner");

        ArgumentCaptor<CashDrawerClose> cap = ArgumentCaptor.forClass(CashDrawerClose.class);
        verify(cashDrawerCloseRepository).save(cap.capture());
        CashDrawerClose saved = cap.getValue();
        assertThat(saved.getBusinessDate()).isEqualTo(LocalDate.of(2026, 6, 13));
        assertThat(saved.getTenantId()).isEqualTo("shop1");
        assertThat(saved.getExpectedAmount()).isEqualByComparingTo("13000000");
        assertThat(saved.getDifferenceAmount()).isEqualByComparingTo("1000000");
        assertThat(saved.getClosedBy()).isEqualTo("owner");
    }

    @Test
    @DisplayName("non-pawn shop: pawn flows excluded from expected")
    void preview_noPawn() {
        when(cashDrawerCloseRepository.findByBusinessDateAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(cashDrawerCloseRepository.findTopByBusinessDateLessThanAndDeletedFalseOrderByBusinessDateDesc(any()))
                .thenReturn(Optional.empty());
        stubCashFlows();
        when(featureContext.hasFeature("PAWN")).thenReturn(false);

        CashDrawerDTO dto = service.getReconciliation(null);

        assertThat(dto.getOpening()).isEqualByComparingTo("0");
        assertThat(dto.getPawnRedeemed()).isEqualByComparingTo("0");
        assertThat(dto.getPawnLoans()).isEqualByComparingTo("0");
        // 0 + 20 + 0 − 8 − 0 − 2 = 10M
        assertThat(dto.getExpected()).isEqualByComparingTo("10000000");
        assertThat(dto.isPawnEnabled()).isFalse();
        verifyNoInteractions(pawnRepository);
    }
}
