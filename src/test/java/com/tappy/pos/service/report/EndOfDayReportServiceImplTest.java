package com.tappy.pos.service.report;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.model.dto.report.EndOfDayReportDTO;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EndOfDayReportServiceImpl Unit Tests")
class EndOfDayReportServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PawnRepository pawnRepository;
    @Mock private FeatureContext featureContext;

    @InjectMocks private EndOfDayReportServiceImpl service;

    private void stubGold() {
        when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(new BigDecimal("45000000"));
        when(orderRepository.goldSoldSummary(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ 3L, new BigDecimal("12.5") }));
        when(orderRepository.sumBuyAmountByDateRange(any(), any())).thenReturn(new BigDecimal("28000000"));
        when(orderRepository.goldBoughtSummary(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ 2L, new BigDecimal("8.0") }));
    }

    @Test
    @DisplayName("pawn shop: sums gold + pawn into cash in/out and net")
    void pawnEnabled_computesNet() {
        stubGold();
        when(featureContext.hasFeature("PAWN")).thenReturn(true);
        when(pawnRepository.countNewPawnsByDateRange(any(), any())).thenReturn(4L);
        when(pawnRepository.sumNewPawnAmountByDateRange(any(), any())).thenReturn(new BigDecimal("15000000"));
        when(pawnRepository.sumByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(List.<Object[]>of(new Object[]{ new BigDecimal("10000000"), 1L }));
        when(pawnRepository.sumInterestAmountByPawnStatusInAndRedeemDateBetweenOrForfeitedDateBetween(anyList(), any(), any(), eq(false)))
                .thenReturn(8500000L);

        EndOfDayReportDTO dto = service.getEndOfDay(LocalDate.of(2026, 6, 13));

        assertThat(dto.getGoldSold().getCount()).isEqualTo(3);
        assertThat(dto.getGoldSold().getWeightChi()).isEqualByComparingTo("12.5");
        assertThat(dto.getGoldSold().getAmount()).isEqualByComparingTo("45000000");
        assertThat(dto.getGoldBought().getWeightChi()).isEqualByComparingTo("8.0");
        assertThat(dto.getPawnNew().getAmount()).isEqualByComparingTo("15000000");
        assertThat(dto.getPawnRedeemed().getAmount()).isEqualByComparingTo("10000000");
        assertThat(dto.getPawnRedeemed().getInterest()).isEqualByComparingTo("8500000");
        // cashIn = 45M sold + 10M principal + 8.5M interest = 63.5M
        assertThat(dto.getTotals().getCashIn()).isEqualByComparingTo("63500000");
        // cashOut = 28M bought + 15M new pawn = 43M
        assertThat(dto.getTotals().getCashOut()).isEqualByComparingTo("43000000");
        assertThat(dto.getTotals().getNet()).isEqualByComparingTo("20500000");
        assertThat(dto.isPawnEnabled()).isTrue();
    }

    @Test
    @DisplayName("non-pawn shop: omits pawn rows; net = gold only")
    void pawnDisabled_omitsPawnRows() {
        stubGold();
        when(featureContext.hasFeature("PAWN")).thenReturn(false);

        EndOfDayReportDTO dto = service.getEndOfDay(null); // defaults to today

        assertThat(dto.isPawnEnabled()).isFalse();
        assertThat(dto.getPawnNew()).isNull();
        assertThat(dto.getPawnRedeemed()).isNull();
        assertThat(dto.getTotals().getCashIn()).isEqualByComparingTo("45000000");
        assertThat(dto.getTotals().getCashOut()).isEqualByComparingTo("28000000");
        assertThat(dto.getTotals().getNet()).isEqualByComparingTo("17000000");
        verifyNoInteractions(pawnRepository);
    }
}
